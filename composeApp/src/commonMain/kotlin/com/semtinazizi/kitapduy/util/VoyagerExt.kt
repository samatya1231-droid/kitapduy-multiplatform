package com.semtinazizi.kitapduy.util

import cafe.adriel.voyager.navigator.Navigator

val Navigator.root: Navigator
    get() = parent?.root ?: this
