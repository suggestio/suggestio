package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.FromJsonT

import scala.scalajs.js.{WrappedDictionary, Dictionary, Any, Array}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 16:15
 * Description: Модели для взаимодействия с Fb.api "/userId/permissions".
 */

case class FbGetPermissionsArgs(
  userId        : String,
  accessToken   : Option[String] = None
)


object FbGetPermissionsResult extends FromJsonT {
  override type T = FbGetPermissionsResult

  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    FbGetPermissionsResult(
      data = d.get("data") match {
        case Some(dataRaw) =>
          dataRaw.asInstanceOf[Array[Any]]
            .iterator
            .flatMap { FbPermissionState.maybeFromJson }
            .toSeq
        case None =>
          Nil
      }
    )
  }
}

/** Ответ на запрос имеющихся пермишшенов. */
case class FbGetPermissionsResult(
  data: Seq[FbPermissionState]
)
