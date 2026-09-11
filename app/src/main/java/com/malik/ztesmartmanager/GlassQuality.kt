package com.malik.ztesmartmanager

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine

internal fun qualityFor(snapshot: RouterSnapshot) = NetworkQualityEngine().score(snapshot)
