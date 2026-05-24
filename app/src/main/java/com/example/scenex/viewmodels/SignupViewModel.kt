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
    var fullName: String = ""
    var email: String = ""
    var phoneNumber: String = ""
    var userName: String = ""
    var password: String = ""
    var age: String = ""
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
     * Executes the Profile Lifecycle logic and Calculates the Intelligence Score.
     * Triggers real-time snapshot listeners for the Web Admin Dashboard.
     */
    private fun syncProfileLifecycle(isFinalSubmit: Boolean = false) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 1. Rule-Based Completeness Scoring System
        var score = 0
        if (fullName.isNotBlank() && email.isNotBlank() && phoneNumber.isNotBlank()) score += 20
        if (height.isNotBlank() && bodyType.isNotBlank() && gender.isNotBlank()) score += 20
        if (spotlightCategory.isNotBlank() && (accents.size + otherSkills.size) >= 3) score += 20
        if (headshotUrl.isNotBlank() && fullBodyUrl.isNotBlank()) score += 20
        if (videoUrl.isNotBlank()) score += 20

        // 2. Profile Approval Workflow States
        val currentStatus = when {
            isFinalSubmit -> "pending_review" 
            score >= 40 -> "active"           
            else -> "draft"                   
        }

        // 3. Structured Firebase Map Payload (Synchronized with Admin Dashboard)
        val profilePayload = hashMapOf(
            "status" to currentStatus,
            "completenessScore" to score,
            "name" to fullName,
            "email" to email,
            "physicalSpecs" to "Height: $height | Build: $bodyType | Gender: $gender",
            "showreelUrl" to videoUrl,
            "updatedAt" to Timestamp.now()
        )

        // 4. Asynchronous Firestore Atomic Update
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

    fun uploadMedia(file: File, type: String) {
        val statusLiveData = if (type == "HEADSHOT") headshotStatus else fullBodyStatus
        statusLiveData.value = "Uploading..."
        
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        imgBBService.uploadImage(IMGBB_API_KEY, body).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()?.data?.url ?: ""
                    if (type == "HEADSHOT") { headshotUrl = url; headshotStatus.value = "Head-shot ✅" }
                    else { fullBodyUrl = url; fullBodyStatus.value = "Full Body ✅" }
                } else {
                    statusLiveData.value = "Failed ❌"
                }
            }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                statusLiveData.value = "Error ❌"
            }
        })
    }

    fun addPortfolioImage(file: File) {
        _isUploading.value = true
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        imgBBService.uploadImage(IMGBB_API_KEY, body).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                _isUploading.value = false
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()?.data?.url
                    url?.let {
                        val currentList = _portfolioImages.value ?: mutableListOf()
                        currentList.add(it)
                        _portfolioImages.value = currentList
                    }
                }
            }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                _isUploading.value = false
            }
        })
    }

    fun createAccount() {
        val profile = UserProfile(
            fullName = fullName, email = email, stageName = userName, role = "TALENT",
            profileImage = _profileImageUrl.value ?: "", phoneNumber = phoneNumber,
            age = age, gender = gender, province = province, city = city,
            relationshipStatus = relationshipStatus, hobbies = hobbies, bio = shortBio
        )
        repository.signupUser(profile, password) { success, error ->
            if (success) _navigateToNextStep.value = "STEP2" else _errorMessage.value = error
        }
    }
    
    fun updateSpotlightAndNavigate() {
        if (spotlightCategory.isEmpty()) {
            _errorMessage.value = "Please select your primary craft"
            return
        }
        repository.saveProfessionalProfile(mapOf("spotlightCategory" to spotlightCategory)) { success ->
            if (success) _navigateToNextStep.value = "STEP3" else _errorMessage.value = "Update failed"
        }
    }

    fun saveFoundationAndNavigate() {
        val updates = hashMapOf<String, Any>(
            "highest_qualification" to qualification,
            "languages" to languages,
            "experience_level" to experience,
            "portfolioLink" to portfolioLink,
            "socialMediaLinks" to socialMediaLinks
        )
        repository.saveProfessionalProfile(updates) { success ->
            if (success) {
                _navigateToNextStep.value = if (spotlightCategory == "Actor" || spotlightCategory == "Model") "ACTOR_SPECS" else "STEP5"
            } else {
                _errorMessage.value = "Failed to save foundation data"
            }
        }
    }

    fun saveActorSpecsAndNavigate() {
        val heightInt = height.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        val specs = hashMapOf<String, Any>(
            "height_cm" to heightInt,
            "hairColor" to hairColor,
            "eye_color" to eyeColor,
            "build_enum" to bodyType,
            "accents" to accents,
            "otherSkills" to otherSkills
        )
        val assets = hashMapOf<String, Any>(
            "headshotUrl" to headshotUrl,
            "fullBodyUrl" to fullBodyUrl,
            "videoUrl" to videoUrl
        )

        repository.saveTalentSpecs(specs) { specsSuccess ->
            if (specsSuccess) {
                repository.saveMediaAssets(assets) { assetsSuccess ->
                    if (assetsSuccess) {
                        syncProfileLifecycle(isFinalSubmit = false) 
                        _navigateToNextStep.value = "STEP5"
                    }
                }
            }
        }
    }

    fun finalizeRegistration() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val portfolioData = mapOf("portfolioImages" to (_portfolioImages.value ?: emptyList<String>()))
        
        repository.saveMediaAssets(portfolioData) {
            syncProfileLifecycle(isFinalSubmit = true) 
            _navigateToNextStep.value = "FINISH"
        }
    }

    fun clearNavigation() { _navigateToNextStep.value = null }
}
