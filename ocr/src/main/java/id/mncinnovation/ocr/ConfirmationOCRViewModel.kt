package id.mncinnovation.ocr

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import id.mncinnovation.ocr.model.KTPModel

class ConfirmationOCRViewModel : ViewModel() {
    private var _isShouldEnableBtnNext = MutableLiveData<Boolean>()
    val isShouldEnableBtnNext: LiveData<Boolean> = _isShouldEnableBtnNext
    private var _state = MutableLiveData<StateConfirm>()
    val state: LiveData<StateConfirm> = _state

    init {
        _state.value = StateConfirm.FILL_STATE
    }

    fun checkValues(ktpModel: KTPModel) {
        _isShouldEnableBtnNext.postValue(
            !(ktpModel.nik.isNullOrEmpty() || (ktpModel.nik?.length
                ?: 0) < 16 || ktpModel.nama.isNullOrEmpty() || ktpModel.tempatLahir.isNullOrEmpty() || ktpModel.golDarah.isNullOrEmpty() || ktpModel.tglLahir.isNullOrEmpty() || ktpModel.jenisKelamin.isNullOrEmpty() || ktpModel.alamat.isNullOrEmpty() || ktpModel.rt.isNullOrEmpty() || ktpModel.rw.isNullOrEmpty() || ktpModel.kelurahan.isNullOrEmpty() || ktpModel.kecamatan.isNullOrEmpty() || ktpModel.agama.isNullOrEmpty() || ktpModel.statusPerkawinan.isNullOrEmpty() || ktpModel.pekerjaan.isNullOrEmpty() || ktpModel.kewarganegaraan.isNullOrEmpty() || ktpModel.berlakuHingga.isNullOrEmpty() || ktpModel.provinsi.isNullOrEmpty() || ktpModel.kabKot.isNullOrEmpty())
        )
    }

    fun updateState() {
        val state = if (_state.value == StateConfirm.FILL_STATE) {
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