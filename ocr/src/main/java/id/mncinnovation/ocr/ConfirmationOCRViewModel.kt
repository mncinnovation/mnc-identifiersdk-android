package id.mncinnovation.ocr

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import id.mncinnovation.ocr.model.OCRValue

class ConfirmationOCRViewModel : ViewModel() {
    private var _isShouldEnableBtnNext = MutableLiveData<Boolean>()
    val isShouldEnableBtnNext: LiveData<Boolean> = _isShouldEnableBtnNext
    private var _state = MutableLiveData<StateConfirm>()
    val state: LiveData<StateConfirm> = _state

    init {
        _state.value = StateConfirm.FILL_STATE
    }

    fun checkValues(ocrValue: OCRValue) {
        _isShouldEnableBtnNext.postValue(!(ocrValue.nik.isNullOrEmpty() || ocrValue.nama.isNullOrEmpty() || ocrValue.tempatLahir.isNullOrEmpty() || ocrValue.golDarah.isNullOrEmpty() || ocrValue.tglLahir.isNullOrEmpty() || ocrValue.jenisKelamin.isNullOrEmpty() || ocrValue.alamat.isNullOrEmpty() || ocrValue.rt.isNullOrEmpty() || ocrValue.rw.isNullOrEmpty() || ocrValue.kelurahan.isNullOrEmpty() || ocrValue.kecamatan.isNullOrEmpty() || ocrValue.agama.isNullOrEmpty() || ocrValue.statusPerkawinan.isNullOrEmpty() || ocrValue.pekerjaan.isNullOrEmpty() || ocrValue.kewarganegaraan.isNullOrEmpty() || ocrValue.berlakuHingga.isNullOrEmpty() || ocrValue.provinsi.isNullOrEmpty() || ocrValue.kabKot.isNullOrEmpty()))
    }

    fun updateState() {
        val state = if(_state.value == StateConfirm.FILL_STATE){
            StateConfirm.CONFIRM_STATE
        } else {
            StateConfirm.FILL_STATE
        }
        _state.postValue(state)
    }
}

enum class StateConfirm {
    FILL_STATE,
    CONFIRM_STATE
}