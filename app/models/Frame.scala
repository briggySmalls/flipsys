package models

import services.StreamTypes.DisplayPayload

case class Frame(images: Seq[DisplayPayload])
