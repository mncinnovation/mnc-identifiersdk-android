package id.mncinnovation.identification.core.common

import android.view.View

fun Boolean?.toVisibilityOrGone(): Int {
    return if (this == true) {
        View.VISIBLE
    } else View.GONE
}
