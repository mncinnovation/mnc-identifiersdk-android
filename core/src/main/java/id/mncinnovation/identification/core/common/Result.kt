package id.mncinnovation.identification.core.common


abstract class Result{
    abstract val isSuccess: Boolean
    abstract val errorMessage: String?
}