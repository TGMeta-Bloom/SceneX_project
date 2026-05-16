package com.example.scenex.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.models.ImgBBResponse
import com.example.scenex.models.UserProfile
import com.example.scenex.network.ImgBBService
import com.example.scenex.repository.UserRepository
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

    // Media Upload Statuses for Step 4 Progress feedback
    val headshotStatus = MutableLiveData<String>("Head-shot")
    val fullBodyStatus = MutableLiveData<String>("Full Body")
    val videoStatus = MutableLiveData<String>("Upload video")
    val audioStatus = MutableLiveData<String>("Upload audio")

    // ImgBB API Key
    private val IMGBB_API_KEY = "3555cbd369113d3b670cec87ddc281a3"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.imgbb.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val imgBBService = retrofit.create(ImgBBService::class.java)

    // Location Data Map
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

    // Form Data
    var fullName: String = ""
    var email: String = ""
    var userName: String = ""
    var password: String = ""
    var phoneNumber: String = ""
    var age: String = ""
    var gender: String = ""
    var province: String = ""
    var city: String = ""
    var relationshipStatus: String = ""
    var hobbies: String = ""
    var shortBio: String = ""
    
    // Step 2 Data
    var spotlightCategory: String = ""

    // Step 3 Data
    var qualification: String = ""
    var languages: String = ""
    var experience: String = ""
    var portfolioLink: String = ""
    var socialMediaLinks: String = ""

    // Step 4 Data (Actor Physical Specs + Specialized Media)
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

    // Step 5 Data
    private val _portfolioImages = MutableLiveData<MutableList<String>>(mutableListOf())
    val portfolioImages: LiveData<MutableList<String>> get() = _portfolioImages

    fun onProvinceSelected(provinceName: String) {
        province = provinceName
        _availableCities.value = citiesMap[provinceName] ?: emptyList()
    }

    fun uploadProfilePicture(file: File) {
        _isUploading.value = true
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        imgBBService.uploadImage(IMGBB_API_KEY, body).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                _isUploading.value = false
                if (response.isSuccessful && response.body()?.success == true) {
                    _profileImageUrl.value = response.body()?.data?.url
                } else {
                    _errorMessage.value = "Image upload failed: ${response.message()}"
                }
            }

            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                _isUploading.value = false
                _errorMessage.value = "Network error: ${t.message}"
            }
        })
    }

    // Specialized Media Uploads for Step 4
    fun uploadMedia(file: File, type: String) {
        val statusLiveData = when(type) {
            "HEADSHOT" -> headshotStatus
            "FULLBODY" -> fullBodyStatus
            "VIDEO" -> videoStatus
            "AUDIO" -> audioStatus
            else -> null
        }
        
        statusLiveData?.value = "Uploading..."
        
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        imgBBService.uploadImage(IMGBB_API_KEY, body).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()?.data?.url ?: ""
                    when(type) {
                        "HEADSHOT" -> { headshotUrl = url; headshotStatus.value = "Head-shot ✅" }
                        "FULLBODY" -> { fullBodyUrl = url; fullBodyStatus.value = "Full Body ✅" }
                        "VIDEO" -> { videoUrl = url; videoStatus.value = "Video ✅" }
                        "AUDIO" -> { audioUrl = url; audioStatus.value = "Audio ✅" }
                    }
                } else {
                    statusLiveData?.value = "Failed ❌"
                    _errorMessage.value = "Upload failed"
                }
            }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                statusLiveData?.value = "Error ❌"
                _errorMessage.value = "Network error"
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
                } else {
                    _errorMessage.value = "Portfolio upload failed"
                }
            }

            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                _isUploading.value = false
                _errorMessage.value = "Network error"
            }
        })
    }

    fun createAccount() {
        if (email.isEmpty() || password.isEmpty() || userName.isEmpty()) {
            _errorMessage.value = "Please fill in all required fields"
            return
        }

        val profile = UserProfile(
            fullName = fullName,
            email = email,
            stageName = userName,
            role = "TALENT",
            profileImage = _profileImageUrl.value ?: "",
            phoneNumber = phoneNumber,
            age = age,
            gender = gender,
            province = province,
            city = city,
            relationshipStatus = relationshipStatus,
            hobbies = hobbies,
            bio = shortBio
        )

        repository.signupUser(profile, password) { success, error ->
            if (success) {
                _navigateToNextStep.value = "STEP2"
            } else {
                _errorMessage.value = error ?: "Signup failed"
            }
        }
    }
    
    fun updateSpotlightAndNavigate() {
        if (spotlightCategory.isEmpty()) {
            _errorMessage.value = "Please select your primary craft"
            return
        }
        
        repository.updateUserField("spotlightCategory", spotlightCategory) { success ->
            if (success) {
                _navigateToNextStep.value = "STEP3"
            } else {
                _errorMessage.value = "Failed to update profile"
            }
        }
    }

    fun saveFoundationAndNavigate() {
        val updates = hashMapOf<String, Any>(
            "qualification" to qualification,
            "languages" to languages,
            "experience" to experience,
            "portfolioLink" to portfolioLink,
            "socialMediaLinks" to socialMediaLinks
        )
        
        repository.updateUserFields(updates) { success ->
            if (success) {
                _navigateToNextStep.value = when(spotlightCategory) {
                    "Actor", "Model" -> "ACTOR_SPECS"
                    else -> "STEP5"
                }
            } else {
                _errorMessage.value = "Failed to save data"
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
            "headshotUrl" to headshotUrl,
            "fullBodyUrl" to fullBodyUrl,
            "videoUrl" to videoUrl,
            "audioUrl" to audioUrl
        )
        
        repository.updateUserFields(updates) { success ->
            if (success) {
                _navigateToNextStep.value = "STEP5"
            } else {
                _errorMessage.value = "Failed to save physical specs"
            }
        }
    }

    fun finalizeRegistration() {
        val finalUpdates = hashMapOf<String, Any>(
            "portfolioImages" to (_portfolioImages.value ?: emptyList<String>()),
            "registrationComplete" to true
        )
        
        repository.updateUserFields(finalUpdates) { success ->
            if (success) {
                _navigateToNextStep.value = "FINISH"
            } else {
                _errorMessage.value = "Failed to complete registration"
            }
        }
    }
    
    fun clearNavigation() {
        _navigateToNextStep.value = null
    }
}
