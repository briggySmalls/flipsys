package services

import models.Image

object StreamTypes {
  type DisplayPayload = (String, Image)
}
