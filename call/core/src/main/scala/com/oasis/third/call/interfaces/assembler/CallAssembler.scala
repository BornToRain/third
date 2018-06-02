package com.oasis.third.call.interfaces.assembler

import com.oasis.third.call.application.service.CallService.CallDTO
import com.oasis.third.call.domain.Call
import org.ryze.micro.core.tool.DateTool

object CallAssembler
{
  @inline
  final def toDTO(d: Call) = CallDTO(
    id        = d._id,
    thirdId   = d.thirdId,
    callState = d.status,
    state     = d.eventStatus,
    callTime  = d.callTime,
    beginTime = d.beginTime map (DateTool formatDate _),
    endTime   = d.endTime map (DateTool formatDate _)
  )
}
