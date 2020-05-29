package com.qadomy.eatitserver.eventbus

import com.qadomy.eatitserver.model.AddonModel

class UpdateAddonModel {

    /**
     *
     *  This class will use for tell activity known when update addon
     */

    var addonModelList: List<AddonModel>? = null

    constructor()
    constructor(addonModelList: List<AddonModel>?) {
        this.addonModelList = addonModelList
    }
}
