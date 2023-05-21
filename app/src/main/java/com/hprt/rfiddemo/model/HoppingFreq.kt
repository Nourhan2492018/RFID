package com.hprt.rfiddemo.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class HoppingFreq(val freq: Int, var checked: Boolean = false) : Parcelable
