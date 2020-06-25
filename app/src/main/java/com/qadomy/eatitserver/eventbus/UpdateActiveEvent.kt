package com.qadomy.eatitserver.eventbus

import com.qadomy.eatitserver.model.ShipperModel

class UpdateActiveEvent(var shipperModel: ShipperModel, var active: Boolean)
