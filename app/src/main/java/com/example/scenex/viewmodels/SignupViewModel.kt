package com.example.scenex.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.models.ImgBBResponse
import com.example.scenex.models.UserProfile
import com.example.scenex.network.ImgBBService
import com.example.scenex.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class SignupViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _signupStatus = MutableLiveData<Boolean>()
    val signupStatus: LiveData<Boolean> get() = _signupStatus

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _profileImageUrl = MutableLiveData<String?>()
    val profileImageUrl: LiveData<String?> get() = _profileImageUrl

    private val _isUploading = MutableLiveData<Boolean>(false)
    val isUploading: LiveData<Boolean> get() = _isUploading

    private val _navigateToNextStep = MutableLiveData<String?>()
    val navigateToNextStep: LiveData<String?> get() = _navigateToNextStep

    // Media Statuses
    val headshotStatus = MutableLiveData<String>("Head-shot")
    val fullBodyStatus = MutableLiveData<String>("Full Body")
    val verificationDocStatus = MutableLiveData<String>("Upload Box")

    private val IMGBB_API_KEY = "113d28dab202d082d441249d7debf1f7"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.imgbb.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val imgBBService = retrofit.create(ImgBBService::class.java)

    private var calibrationWeights: Map<String, Any>? = null

    init {
        repository.getRankingCalibration { weights ->
            calibrationWeights = weights
            Log.d("SceneX_Calibration", "✅ Weights Loaded: v${weights?.get("rankingVersion") ?: 1}")
        }
    }

    // Location Data for Sri Lankan context
    val provinces = listOf("Western Province", "Central Province", "Southern Province", "Northern Province", "Eastern Province", "North Western Province", "North Central Province", "Uva Province", "Sabaragamuwa Province")
    private val citiesMap = mapOf(
        "Western Province" to listOf("Colombo", "Dehiwala", "Moratuwa", "Sri Jayawardenepura Kotte", "Negombo", "Panadura", "Kalutara", "Horana", "Gampaha", "Wattala", "Kelaniya"),
        "Central Province" to listOf("Kandy", "Matale", "Nuwara Eliya", "Gampola", "Hatton", "Dambulla"),
        "Southern Province" to listOf("Galle", "Matara", "Hambantota", "Tangalle", "Ambalangoda", "Weligama"),
        "Northern Province" to listOf("Jaffna", "Kilinochchi", "Mullaitivu", "Vavuniya", "Mannar"),
        "Eastern Province" to listOf("Trincomalee", "Batticaloa", "Kalmunai", "Ampara"),
        "North Western Province" to listOf("Kurunegala", "Puttalam", "Chilaw", "Kuliyapitiya"),
        "North Central Province" to listOf("Anuradhapura", "Polonnaruwa", "Kekirawa"),
        "Uva Province" to listOf("Badulla", "Bandarawela", "Ella"),
        "Sabaragamuwa Province" to listOf("Ratnapura", "Kegalle", "Balangoda", "Embilipitiya")
    )

    private val _availableCities = MutableLiveData<List<String>>()
    val availableCities: LiveData<List<String>> get() = _availableCities

    fun onProvinceSelected(provinceName: String) {
        province = provinceName
        _availableCities.value = citiesMap[provinceName] ?: emptyList()
    }

    // --- FORM DATA STATE ---
    var userRole: String = "TALENT" 
    var fullName: String = ""
    var email: String = ""
    var phoneNumber: String = ""
    var userName: String = ""
    var password: String = ""
    var age: Int = 0 
    var gender: String = ""
    var province: String = ""
    var city: String = ""
    var relationshipStatus: String = ""
    var hobbies: String = ""
    var shortBio: String = ""
    
    var spotlightCategory: String = ""
    var qualification: String = ""
    var languages: String = ""
    var experience: String = ""
    var portfolioLink: String = ""
    var socialMediaLinks: String = ""

    var companyName: String = ""
    var industryProofLinks: List<String> = emptyList()
    var nicImageUrl: String = ""

    var height: String = ""
    var hairColor: String = ""
    var eyeColor: String = ""
    var bodyType: String = ""
    var accents: List<String> = emptyList()
    var otherSkills: List<String> = emptyList()
    var headshotUrl: String = ""
    var fullBodyUrl: String = ""
    var videoUrl: String = ""
    var audioUrl: String = ""

    private val _portfolioImages = MutableLiveData<MutableList<String>>(mutableListOf())
    val portfolioImages: LiveData<MutableList<String>> get() = _portfolioImages

    private fun calculateWeightedCompletion(): Int {
        var score = 0
        if (userRole == "TALENT") {
            if (fullName.isNotBlank() && email.isNotBlank() && phoneNumber.isNotBlank()) score += 10
            if (height.isNotBlank() && age > 0 && gender.isNotBlank()) score += 10
            if (spotlightCategory.isNotBlank() && languages.isNotBlank() && experience.isNotBlank()) score += 20
            if (headshotUrl.isNotBlank() && fullBodyUrl.isNotBlank()) score += 30
            if (videoUrl.isNotBlank()) score += 30
        } else {
            if (fullName.isNotBlank() && email.isNotBlank()) score += 10 
            if (spotlightCategory.isNotBlank()) score += 20 
            if (companyName.isNotBlank()) score += 20 
            if (industryProofLinks.isNotEmpty()) score += 30 
            if (nicImageUrl.isNotBlank()) score += 20 
        }
        return score.coerceAtMost(100)
    }

    private fun calculateRankingScore(completeness: Int): Double {
        val cw = calibrationWeights ?: mapOf(
            "completenessWeight" to 15L,
            "experienceWeight" to 18L,
            "skillsWeight" to 25L,
            "portfolioWeight" to 30L
        )
        val wC = (cw["completenessWeight"] as? Number)?.toDouble() ?: 15.0
        val wE = (cw["experienceWeight"] as? Number)?.toDouble() ?: 18.0
        val wS = (cw["skillsWeight"] as? Number)?.toDouble() ?: 25.0
        val wP = (cw["portfolioWeight"] as? Number)?.toDouble() ?: 30.0

        val completenessYield = completeness.toDouble()
        val skillsYield = Math.min(100.0, (accents.size + otherSkills.size) * 20.0) 
        val experienceYield = if (experience.isNotBlank()) 100.0 else 0.0
        val portfolioYield = Math.min(100.0, ((if (portfolioLink.isNotBlank()) 50 else 0) + (_portfolioImages.value?.size ?: 0) * 10).toDouble())

        val numerator = (portfolioYield * wP) + (skillsYield * wS) + (experienceYield * wE) + (completenessYield * wC)
        val denominator = wP + wS + wE + wC
        val finalScore = if (denominator > 0) numerator / denominator else completenessYield
        return finalScore.coerceAtMost(100.0)
    }

    private fun syncProfileLifecycle(isFinalSubmit: Boolean = false) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val completion = calculateWeightedCompletion()
        val rankingDecimal = calculateRankingScore(completion)

        val featuredThreshold = (calibrationWeights?.get("featuredThreshold") as? Number)?.toInt() ?: 80
        val normalThreshold = (calibrationWeights?.get("normalThreshold") as? Number)?.toInt() ?: 50

        val tier = when {
            rankingDecimal >= featuredThreshold -> "FEATURED"
            rankingDecimal >= normalThreshold -> "NORMAL"
            else -> "BASIC"
        }

        val currentStatus = when {
            isFinalSubmit && completion >= 70 -> "pending_review" 
            completion >= 70 -> "eligible_for_review"
            completion >= 40 -> "active"
            else -> "draft"
        }

        val accountProfilePicture = _profileImageUrl.value ?: ""

        val profilePayload = mutableMapOf<String, Any>(
            "status" to currentStatus,
            "completenessScore" to completion.toLong(),
            "rankingScore" to rankingDecimal.toLong(),
            "calculated_score" to rankingDecimal,
            "visibility_tier" to tier,
            "fullName" to fullName,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "age" to age,
            "gender" to gender,
            "province" to province,
            "city" to city,
            "userRole" to userRole,
            "rankingVersion" to (calibrationWeights?.get("rankingVersion") ?: 1),
            "verificationStatus" to if (currentStatus == "pending_review") "pending" else "unverified",
            "updatedAt" to Timestamp.now()
        )

        if (accountProfilePicture.isNotEmpty()) {
            profilePayload["profileImage"] = accountProfilePicture
        }
        
        if (userRole == "RECRUITER") {
            profilePayload["companyName"] = companyName
            profilePayload["industryProofLinks"] = industryProofLinks
            profilePayload["nicImageUrl"] = nicImageUrl
        } else {
            profilePayload["physicalSpecs"] = "Height: $height | Build: $bodyType | Gender: $gender"
            profilePayload["showreelUrl"] = videoUrl
            profilePayload["spotlightCategory"] = spotlightCategory
            profilePayload["headshotUrl"] = headshotUrl 
            profilePayload["fullBodyUrl"] = fullBodyUrl 
        }

        FirebaseFirestore.getInstance().collection("profiles").document(userId)
            .set(profilePayload, SetOptions.merge())
            .addOnSuccessListener { Log.d("SceneX_Engine", "✅ Profile Sync successful") }
    }

    fun finalizeRegistration() {
        val score = calculateWeightedCompletion()
        
        if (userRole == "RECRUITER") {
            if (score < 70) {
                _errorMessage.value = "Recruiter profile strength: $score%. Minimum 70% required."
                return
            }
        } else {
            if (score < 40) {
                _errorMessage.value = "Talent profile strength: $score%. Minimum 40% required."
                return
            }
        }

        syncProfileLifecycle(isFinalSubmit = true)
        _navigateToNextStep.value = "FINISH"
    }

    fun uploadProfilePicture(file: File) {
        _isUploading.value = true
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        imgBBService.uploadImage(IMGBB_API_KEY, MultipartBody.Part.createFormData("image", file.name, requestFile)).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                _isUploading.value = false
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()?.data?.url
                    _profileImageUrl.value = url
                    url?.let { repository.saveMediaAssets(mapOf("profileImageUrl" to it)) { } }
                    syncProfileLifecycle(false)
                }
            }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) { _isUploading.value = false }
        })
    }

    /**
     * 🧠 INTELLIGENCE ROUTING logic updated:
     * Redirects all performance categories (Actors, Singers, Dancers, etc.) to the Specs screen.
     * Technical crew (Editors, Sound, etc.) are routed directly to the Portfolio section.
     */
    fun saveFoundationAndNavigate() {
        val updates = hashMapOf<String, Any>(
            "highest_qualification" to qualification, 
            "languages" to languages, 
            "experience_level" to experience, 
            "portfolioLink" to portfolioLink, 
            "socialMediaLinks" to socialMediaLinks
        )
        repository.saveProfessionalProfile(updates) { 
            if (it) {
                syncProfileLifecycle(false)
                
                // Categorize based on whether they need physical specs (Performers) or just portfolio (Crew)
                val performers = listOf("Actor", "Model", "Singer", "Dancer", "News Anchor", "Voice Artist", "Presenter")
                
                _navigateToNextStep.value = if (performers.contains(spotlightCategory)) {
                    "ACTOR_SPECS" 
                } else {
                    "STEP5" 
                }
            }
        }
    }

    fun saveActorSpecsAndNavigate() {
        val updates = hashMapOf<String, Any>(
            "height" to height,
            "hairColor" to hairColor,
            "eyeColor" to eyeColor,
            "bodyType" to bodyType,
            "accents" to accents,
            "otherSkills" to otherSkills,
            "videoUrl" to videoUrl,
            "audioUrl" to audioUrl
        )
        repository.saveProfessionalProfile(updates) { 
            if (it) {
                syncProfileLifecycle(false)
                _navigateToNextStep.value = "STEP5" 
            }
        }
    }

    fun addPortfolioImage(file: File) {
        _isUploading.value = true
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        imgBBService.uploadImage(IMGBB_API_KEY, MultipartBody.Part.createFormData("image", file.name, requestFile)).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, r: Response<ImgBBResponse>) {
                _isUploading.value = false
                if (r.isSuccessful && r.body()?.success == true) {
                    r.body()?.data?.url?.let { url ->
                        val list = _portfolioImages.value ?: mutableListOf()
                        list.add(url)
                        _portfolioImages.value = list
                    }
                }
            }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) { _isUploading.value = false }
        })
    }

    fun createAccount() {
        val profile = UserProfile(fullName = fullName, email = email, stageName = userName, role = userRole, userRole = userRole, phoneNumber = phoneNumber, age = age, gender = gender)
        repository.signupUser(profile, password) { success, error ->
            if (success) { syncProfileLifecycle(false); _navigateToNextStep.value = if (userRole == "RECRUITER") "RECRUITER_STEP2" else "STEP2" } 
            else _errorMessage.value = error
        }
    }

    fun updateSpotlightAndNavigate() {
        if (spotlightCategory.isEmpty()) { _errorMessage.value = "Select your primary craft"; return }
        repository.saveProfessionalProfile(mapOf("spotlightCategory" to spotlightCategory)) { if (it) { syncProfileLifecycle(false); _navigateToNextStep.value = if (userRole == "RECRUITER") "RECRUITER_STEP3" else "STEP3" } }
    }

    fun saveRecruiterExperienceAndNavigate(company: String, proofLinks: List<String>, exp: String) {
        companyName = company; industryProofLinks = proofLinks; experience = exp
        if (proofLinks.isEmpty()) { _errorMessage.value = "Mandatory: Provide links"; return }
        repository.saveProfessionalProfile(hashMapOf("companyName" to company, "industryProofLinks" to proofLinks, "experience" to exp)) { 
            if (it) { syncProfileLifecycle(false); _navigateToNextStep.value = "RECRUITER_VERIFICATION" } 
        }
    }

    fun uploadMedia(file: File, type: String) {
        val status = if (type == "HEADSHOT") headshotStatus else fullBodyStatus
        status.value = "Uploading..."
        _isUploading.value = true
        imgBBService.uploadImage(IMGBB_API_KEY, MultipartBody.Part.createFormData("image", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, r: Response<ImgBBResponse>) {
                _isUploading.value = false
                if (r.isSuccessful && r.body()?.success == true) {
                    val url = r.body()?.data?.url ?: ""
                    if (type == "HEADSHOT") { headshotUrl = url; headshotStatus.value = "Head-shot ✅" } 
                    else { fullBodyUrl = url; fullBodyStatus.value = "Full Body ✅" }
                    syncProfileLifecycle(false)
                } else status.value = "Failed ❌"
            }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) { _isUploading.value = false; status.value = "Error ❌" }
        })
    }

    fun uploadVerificationDoc(file: File) {
        verificationDocStatus.value = "Uploading..."
        _isUploading.value = true
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        imgBBService.uploadImage(IMGBB_API_KEY, MultipartBody.Part.createFormData("image", file.name, requestFile)).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                _isUploading.value = false
                if (response.isSuccessful && response.body()?.success == true) {
                    nicImageUrl = response.body()?.data?.url ?: ""
                    verificationDocStatus.value = "Verified ✅"
                    syncProfileLifecycle(false)
                } else {
                    verificationDocStatus.value = "Failed ❌"
                }
            }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                _isUploading.value = false
                verificationDocStatus.value = "Error ❌"
            }
        })
    }

    fun clearNavigation() {
        _navigateToNextStep.value = null
    }
}
