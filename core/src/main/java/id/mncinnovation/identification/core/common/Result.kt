package id.mncinnovation.identification.core.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


abstract class Result {
    abstract val isSuccess: Boolean
    abstract val errorMessage: String?
    abstract val errorType: ResultErrorType?
}

@Parcelize
data class ResultImpl(
    override val isSuccess: Boolean,
    override val errorMessage: String?,
    override val errorType: ResultErrorType?
) : Result(), Parcelable