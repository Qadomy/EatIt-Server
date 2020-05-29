package com.qadomy.eatitserver.eventbus

import com.qadomy.eatitserver.model.SizeModel

class UpdateSizeModel {

    /**
     *
     *  This class will use for tell activity known when update size
     */

    var sizeModelList: List<SizeModel>? = null

    constructor()
    constructor(sizeModelList: List<SizeModel>?) {
        this.sizeModelList = sizeModelList
    }
}
