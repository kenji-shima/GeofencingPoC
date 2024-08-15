package com.mapbox.geofencing.model

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.mapbox.geofencing.ui.theme.Black
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class GeofencingViewModel @Inject constructor() : ViewModel(){

    private val _showLocationPanelFlow = MutableStateFlow<Boolean>(false)
    val showLocationPanelFlow: StateFlow<Boolean> = _showLocationPanelFlow.asStateFlow()

    fun setShowLocationPanel(show: Boolean){
        _showLocationPanelFlow.value = show
    }

    private val _isNavigationReadyFlow = MutableStateFlow<Boolean>(false)
    val isNavigationReadyFlow: StateFlow<Boolean> = _isNavigationReadyFlow.asStateFlow()

    fun setNavigationReady(ready: Boolean){
        _isNavigationReadyFlow.value = ready
    }

    private val _infoArticlesFlow = MutableStateFlow<List<InfoArticle>>(emptyList())
    val infoArticlesFlow: StateFlow<List<InfoArticle>> = _infoArticlesFlow.asStateFlow()

    private val _isVisibleStatesFlow = MutableStateFlow<Map<InfoArticle, Boolean>>(emptyMap())
    val isVisibleStatesFlow: StateFlow<Map<InfoArticle, Boolean>> = _isVisibleStatesFlow.asStateFlow()

    private val _isFiredFlow = MutableStateFlow(false)
    val isFiredFlow: StateFlow<Boolean> = _isFiredFlow.asStateFlow()

    private val _particleColorFlow = MutableStateFlow(Black)
    val particleColorFlow: StateFlow<Color> = _particleColorFlow.asStateFlow()

    private val _addedArticleFlow = MutableStateFlow(InfoArticle())
    val addedArticleFlow: StateFlow<InfoArticle> = _addedArticleFlow.asStateFlow()

    private val _idFlow = MutableStateFlow(0)
    val idFlow: StateFlow<Int> = _idFlow.asStateFlow()

    private val _exitedTimesFlow = MutableStateFlow<Map<String,String>>(mapOf())
    val exitedTimesFlow: StateFlow<Map<String,String>> = _exitedTimesFlow.asStateFlow()

    private val _dwellTimesFlow = MutableStateFlow<Map<String,String>>(mapOf())
    val dwelledTimesFlow: StateFlow<Map<String,String>> = _dwellTimesFlow.asStateFlow()

    fun setDwelledTime(id:String, time:String){
        _dwellTimesFlow.value = _dwellTimesFlow.value.toMutableMap().apply {
            put(id, time)
        }
    }

    fun setExitedTime(id: String, time: String){
        _exitedTimesFlow.value = _exitedTimesFlow.value.toMutableMap().apply {
            put(id, time)
        }
    }

    fun setIsVisible(article: InfoArticle, isVisible: Boolean) {
        _isVisibleStatesFlow.value = _isVisibleStatesFlow.value.toMutableMap().apply { this[article] = isVisible }
    }

    fun setIsFired(isFired: Boolean) {
        _isFiredFlow.value = isFired
    }

    fun setParticleColor(color: Color) {
        _particleColorFlow.value = color
    }

    fun addArticleToTop(article: InfoArticle){
        _addedArticleFlow.value = article
        _infoArticlesFlow.value = listOf(article) + _infoArticlesFlow.value
        _isVisibleStatesFlow.value = _isVisibleStatesFlow.value.mapValues { true }
        _isVisibleStatesFlow.value = mapOf(article to false) + _isVisibleStatesFlow.value
    }

    fun incrementId() {
        _idFlow.value += 1
    }

//    fun getArticle(id: String): InfoArticle{
//        return _infoArticlesFlow.value.first { infoArticle -> infoArticle.id == id }
//    }
//
//    fun updateArticle(updatedArticle: InfoArticle) {
//        _infoArticlesFlow.value = _infoArticlesFlow.value.map { article ->
//            if (article.id == updatedArticle.id) {
//                article.copy(exitedTime = updatedArticle.exitedTime)
//            } else {
//                article
//            }
//        }
//    }
//
//    fun updateArticles() {
//        val article = InfoArticle()
//        _infoArticlesFlow.value = _infoArticlesFlow.value + article
//        viewModelScope.launch {
//            delay(100) // Adjust the delay as needed
//            _infoArticlesFlow.value = _infoArticlesFlow.value - article
//        }
//    }

//    init {
//        _infoItemListFlow.value = emptyList()
//    }

}