package models.msys

import models.{MNodeType, MNode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.10.15 19:16
 * Description: Модели аргументов для вызова шаблона adnNodesListTpl.
 */

/** Аргументы рендера кнопки-ссылки на тип узла. */
case class MNodeTypeInfo(
  name  : String,
  ntype : MNodeType,
  count : Long
)


/** Интерфейс контейнера аргументов вызова шаблона sys/adnNodesListTpl. */
trait ISysNodeListTplArgs {

  /** Узлы. */
  def mnodes: Seq[MNode]

  /** Данные по типам узлов. */
  def ntypes: Seq[MNodeTypeInfo]

  /** Аргументы вызова экшена. */
  def args0 : MSysNodeListArgs

  def total : Long

}


/** Класс контейнера аргументов вызова шаблона sys/adnNodesListTpl. */
case class MSysNodeListTplArgs(
  override val mnodes     : Seq[MNode],
  override val ntypes     : Seq[MNodeTypeInfo],
  override val args0      : MSysNodeListArgs,
  override val total      : Long
)
  extends ISysNodeListTplArgs

