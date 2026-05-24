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

    // Media Upload Statuses
    val headshotStatus = MutableLiveData<String>("Head-shot")
    val fullBodyStatus = MutableLiveData<String>("Full Body")
    val verificationDocStatus = MutableLiveData<String>("Upload Box")

    private val IMGBB_API_KEY = "3555cbd369113d3b670cec87ddc281a3"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.imgbb.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val imgBBService = retrofit.create(ImgBBService::class.java)

    // --- LOCATION DATA ---
    val provinces = listOf(
        "Western Province", "Central Province", "Southern Province", 
        "Northern Province", "Eastern Province", "North Western Province", 
        "North Central Province", "Uva Province", "Sabaragamuwa Province"
    )

    private val citiesMap = mapOf(
        "Western Province" to listOf("Colombo", "Dehiwala", "Moratuwa", "Sri Jayawardenepura Kotte", "Negombo", "Panadura", "Kalutara", "Horana", "Gampaha", "Wattala", "Kelaniya"),
        "Central Province" to listOf("Kandy", "Matale", "Nuwara Eliya", "Gampola", "Hatton", "Dambulla"),
        "Southern Province" to listOf("Galle", "Matara", "Hambantota", "Tangalle", "Ambalangoda", "Weligama"),
        "Northern Province" to listOf("Jaffna", "Kilinochchi", "Mullaitivu", "Vavuniya", "Mannar"),
        "Eastern Province" to listOf("Trincomalee", "Batticaloa", "Kalmunai", "Ampara"),
        "North Western Province" to listOf("Kurunegala", "Puttalam", "Chilaw", "Kuliyapitiya"),
        "North Central Province" to listOf("Anuradhapura", "Polonnaruwa", "Kekirawa"),
        "Uva Province" to listOf("Badulla", "Bandarawela", "Monaragala", "Ella"),
        "Sabaragamuwa Province" to listOf("Ratnapura", "Kegalle", "Balangoda", "Embilipitiya")
    )

    private val _availableCities = MutableLiveData<List<String>>()
    val availableCities: LiveData<List<String>> get() = _availableCities

    fun onProvinceSelected(provinceName: String) {
        province = provinceName
        _availableCities.value = citiesMap[provinceName] ?: emptyList()
    }

    // Form Data Variables
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

    // Recruiter Specific
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

    /**
     * Senior Fix: Updated Payload mapping to include 'age', 'gender', and all Step 1 fields
     * in every lifecycle sync to ensure they appear and update in the Firebase console.
     */
    private fun syncProfileLifecycle(isFinalSubmit: Boolean = false) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        var score = 0
        if (fullName.isNotBlank() && email.isNotBlank() && phoneNumber.isNotBlank()) score += 20
        
        if (userRole == "TALENT") {
            if (height.isNotBlank() && bodyType.isNotBlank() && gender.isNotBlank()) score += 20
            if (spotlightCategory.isNotBlank() && (accents.size + otherSkills.size) >= 3) score += 20
            if (headshotUrl.isNotBlank() && fullBodyUrl.isNotBlank()) score += 20
            if (videoUrl.isNotBlank()) score += 20
        } else {
            if (spotlightCategory.isNotBlank()) score += 20
            if (companyName.isNotBlank()) score += 20
            if (industryProofLinks.isNotEmpty()) score += 20
            if (nicImageUrl.isNotBlank()) score += 20
        }

        val currentStatus = when {
            isFinalSubmit -> "pending_review" 
            score >= 40 -> "active"           
            else -> "draft"                   
        }

        val profilePayload = mutableMapOf<String, Any>(
            "status" to currentStatus,
            "completenessScore" to score,
            "name" to fullName,
            "fullName" to fullName,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "age" to age, // PERSISTENCE FIX: Added age to sync
            "gender" to gender,
            "province" to province,
            "city" to city,
            "relationshipStatus" to relationshipStatus,
            "hobbies" to hobbies,
            "bio" to shortBio,
            "userRole" to userRole,
            "updatedAt" to Timestamp.now()
        )
        
        if (userRole == "RECRUITER") {
            profilePayload["companyName"] = companyName
            profilePayload["industryProofLinks"] = industryProofLinks
            profilePayload["nicImageUrl"] = nicImageUrl
            profilePayload["verificationStatus"] = currentStatus
        } else {
            profilePayload["physicalSpecs"] = "Height: $height | Build: $bodyType | Gender: $gender"
            profilePayload["showreelUrl"] = videoUrl
        }

        FirebaseFirestore.getInstance().collection("profiles").document(userId)
            .set(profilePayload, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("SceneX_Lifecycle", "✅ Sync Success: $currentStatus | Score: $score")
            }
            .addOnFailureListener { e ->
                Log.e("SceneX_Lifecycle", "❌ Sync Failed: ${e.message}")
            }
    }

    fun uploadProfilePicture(file: File) {
        _isUploading.value = true
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        imgBBService.uploadImage(IMGBB_API_KEY, body).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                _isUploading.value = false
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()?.data?.url
                    _profileImageUrl.value = url
                    url?.let { repository.saveMediaAssets(mapOf("profileImageUrl" to it)) { } }
                }
            }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                _isUploading.value = false
            }
        })
    }

    fun uploadVerificationDoc(file: File) {
        _isUploading.value = true
        verificationDocStatus.value = "Uploading..."
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        imgBBService.uploadImage(IMGBB_API_KEY, body).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                _isUploading.value = false
                if (response.isSuccessful && response.body()?.success == true) {
                    nicImageUrl = response.body()?.data?.url ?: ""
                    verificationDocStatus.value = "Uploaded ✅"
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

    fun createAccount() {
        val profile = UserProfile(
            fullName = fullName, email = email, stageName = userName, role = userRole,
            profileImage = _profileImageUrl.value ?: "", phoneNumber = phoneNumber,
            age = age, gender = gender, province = province, city = city,
            relationshipStatus = relationshipStatus, hobbies = hobbies, bio = shortBio
        )
        repository.signupUser(profile, password) { success, error ->
            if (success) {
                syncProfileLifecycle(false) 
                _navigateToNextStep.value = if (userRole == "RECRUITER") "RECRUITER_STEP2" else "STEP2"
            } else {
                _errorMessage.value = error
            }
        }
    }
    
    fun updateSpotlightAndNavigate() {
        if (spotlightCategory.isEmpty()) {
            _errorMessage.value = "Please select your primary craft"
            return
        }
        repository.saveProfessionalProfile(mapOf("spotlightCategory" to spotlightCategory)) { success ->
            if (success) {
                syncProfileLifecycle(false) 
                _navigateToNextStep.value = if (userRole == "RECRUITER") "RECRUITER_STEP3" else "STEP3"
            } else {
                _errorMessage.value = "Update failed"
            }
        }
    }

    fun saveRecruiterExperienceAndNavigate(company: String, proofLinks: List<String>, exp: String) {
        companyName = company
        industryProofLinks = proofLinks
        experience = exp
        
        if (proofLinks.isEmpty()) {
            _errorMessage.value = "Please provide at least one industry proof link"
            return
        }

        val updates = hashMapOf<String, Any>(
            "companyName" to company,
            "industryProofLinks" to proofLinks,
            "experience" to exp
        )
        
        repository.saveProfessionalProfile(updates) { success ->
            if (success) {
                syncProfileLifecycle(false) 
                _navigateToNextStep.value = "RECRUITER_VERIFICATION"
            } else {
                _errorMessage.value = "Save failed"
            }
        }
    }

    fun finalizeRecruiterSignup() {
        if (industryProofLinks.isEmpty()) {
            _errorMessage.value = "Please provide at least one industry proof link"
            return
        }
        syncProfileLifecycle(isFinalSubmit = true)
        _navigateToNextStep.value = "FINISH"
    }

    // --- TALENT METHODS (PRESERVED) ---

    fun saveFoundationAndNavigate() {
        val updates = hashMapOf<String, Any>("highest_qualification" to qualification, "languages" to languages, "experience_level" to experience, "portfolioLink" to portfolioLink, "socialMediaLinks" to socialMediaLinks)
        repository.saveProfessionalProfile(updates) { if (it) _navigateToNextStep.value = if (spotlightCategory == "Actor" || spotlightCategory == "Model") "ACTOR_SPECS" else "STEP5" }
    }

    fun saveActorSpecsAndNavigate() {
        val h = height.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        repository.saveTalentSpecs(mapOf("height_cm" to h, "hairColor" to hairColor, "eye_color" to eyeColor, "build_enum" to bodyType, "accents" to accents, "otherSkills" to otherSkills)) { if (it) repository.saveMediaAssets(mapOf("headshotUrl" to headshotUrl, "fullBodyUrl" to fullBodyUrl, "videoUrl" to videoUrl)) { syncProfileLifecycle(false); _navigateToNextStep.value = "STEP5" } }
    }

    fun finalizeRegistration() { repository.saveMediaAssets(mapOf("portfolioImages" to (_portfolioImages.value ?: emptyList<String>()))) { syncProfileLifecycle(true); _navigateToNextStep.value = "FINISH" } }

    fun addPortfolioImage(file: File) {
        _isUploading.value = true
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        imgBBService.uploadImage(IMGBB_API_KEY, MultipartBody.Part.createFormData("image", file.name, requestFile)).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, r: Response<ImgBBResponse>) { _isUploading.value = false; if (r.isSuccessful && r.body()?.success == true) { r.body()?.data?.url?.let { val list = _portfolioImages.value ?: mutableListOf(); list.add(it); _portfolioImages.value = list } } }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) { _isUploading.value = false }
        })
    }

    fun uploadMedia(file: File, type: String) {
        val status = if (type == "HEADSHOT") headshotStatus else fullBodyStatus
        status.value = "Uploading..."
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        imgBBService.uploadImage(IMGBB_API_KEY, MultipartBody.Part.createFormData("image", file.name, requestFile)).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, r: Response<ImgBBResponse>) { if (r.isSuccessful && r.body()?.success == true) { val url = r.body()?.data?.url ?: ""; if (type == "HEADSHOT") { headshotUrl = url; headshotStatus.value = "Head-shot ✅" } else { fullBodyUrl = url; fullBodyStatus.value = "Full Body ✅" } } else { status.value = "Failed ❌" } }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) { status.value = "Error ❌" }
        })
    }

    fun clearNavigation() { _navigateToNextStep.value = null }
}
