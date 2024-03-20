package com.baes.inspector

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.baes.inspector.location.LocationModel

class MainViewModal: ViewModel() {
    val locUpdates = MutableLiveData<LocationModel>()
    val timeData = MutableLiveData<String>()
}