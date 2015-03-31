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
  def HAS_PERMS_FN  = "p"
  def STEP_FN       = "s"

  /** Десериализация контекста из JSON. */
  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    FbCtx(
      hasPerms = d.get(HAS_PERMS_FN)
        .fold (Seq.empty[FbPermission]) (FbPermissions.permsFromJson),
      step = d.get(STEP_FN)
        .map(_.asInstanceOf[Int])
    )
  }

}


import FbCtx._


case class FbCtx(
  hasPerms    : Seq[FbPermission] = Nil,
  step        : Option[Int]       = None
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
    if (hasPerms.nonEmpty)
      d.update(HAS_PERMS_FN, FbPermissions.permsToJson(hasPerms) )
    if (step.isDefined)
      d.update(STEP_FN, step.get)
    d
  }

}
