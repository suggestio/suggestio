package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.{FromJsonT, IToJsonDict}

import scala.scalajs.js.{WrappedDictionary, Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.15 15:24
 * Description: Контекст Facebook-адаптера. Передается при необходимости в mctx.custom.
 */

object FbCtx extends FromJsonT {

  override type T = FbCtx

  /** Имя сериализованного поля c инфой по таргету. */
  def FB_TG_FN      = "g"
  def HAS_PERMS_FN  = "p"

  /** Десериализация контекста из JSON. */
  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    FbCtx(
      fbTg = d.get(FB_TG_FN)
        .map(FbTarget.fromJson),
      hasPerms = d.get(HAS_PERMS_FN)
        .fold (Seq.empty[FbPermission]) (FbPermissions.permsFromJson)
    )
  }
}


import FbCtx._


case class FbCtx(
  fbTg        : Option[FbTarget]  = None,
  hasPerms    : Seq[FbPermission] = Nil
) extends IToJsonDict {

  def nonEmpty: Boolean = {
    productIterator.exists {
      case opt: Option[_]           => opt.nonEmpty
      case col: TraversableOnce[_]  => col.nonEmpty
      case null => false
      case _    => true
    }
  }
  def isEmpty = !nonEmpty

  /** Сериализация экземпляра контекста в JSON. */
  override def toJson: Dictionary[Any] = {
    val d = Dictionary[Any]()
    if (fbTg.isDefined)
      d.update(FB_TG_FN, fbTg.get.toJson)
    if (hasPerms.nonEmpty)
      d.update(HAS_PERMS_FN, FbPermissions.permsToJson(hasPerms) )
    d
  }

}
