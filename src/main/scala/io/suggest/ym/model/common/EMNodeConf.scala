package io.suggest.ym.model.common

import io.suggest.model.{EsModel, EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.model.EsModel._
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import scala.collection.JavaConversions._
import java.{util => ju, lang => jl}
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.14 9:29
 * Description: Конфиг узла, ключи которого выходят за пределы adn-конфига. Сюда складываются настройки,
 * не подходящие в другие категории.
 * Изначально этот аддон предназначался для хранения id блоков (sioweb21: util.blocks.BlocksConf), к которым
 * дополнительно имеет доступ указанный узел.
 * Также надо куда-то пристроить logoImgIdOpt и meta.welcomeAdId. Поэтому назрела необходимость в conf-поле.
 */
object EMNodeConf {

  val CONF_ESFN = "c"

}


import EMNodeConf._

/** Аддон для статической стороны модели узла. */
trait EMNodeConfStatic extends EsModelStaticMutAkvT {
  override type T <: EMNodeConfMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(CONF_ESFN, enabled = true, properties = NodeConf.generateMappingProps) ::
      super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (CONF_ESFN, cmRaw) =>
        acc.conf = NodeConf.deserialize(cmRaw)
    }
  }
}


/** аддон к динамической модели узла. */
trait EMNodeConf extends EsModelPlayJsonT {
  override type T <: EMNodeConf
  def conf: NodeConf

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc1 = super.writeJsonFields(acc)
    if (conf.nonEmpty)
      CONF_ESFN -> conf.toPlayJson :: acc1
    else
      acc1
  }
}


/** var-версия [[EMNodeConf]]. */
trait EMNodeConfMut extends EMNodeConf {
  override type T <: EMNodeConfMut
  var conf: NodeConf
}



/** Статическая утиль контейнера конфига. */
object NodeConf {
  val WITH_BLOCKS_ESFN = "wb"
  val SHOWCASE_VOID_FILLER_ESFN = "svf"

  /** Дефолтовый неизменяемый инстанс контейнера конфига. Для изменения - перезаписывать поле через copy(). */
  val DEFAULT = NodeConf()

  def generateMappingProps: List[DocField] = List(
    FieldNumber(WITH_BLOCKS_ESFN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(SHOWCASE_VOID_FILLER_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
  )

  val deserialize: PartialFunction[Any, NodeConf] = {
    case jmap: ju.Map[_,_] =>
      NodeConf(
        withBlocks = Option(jmap get WITH_BLOCKS_ESFN).fold[Set[Int]] (Set.empty) {
          case l: jl.Iterable[_] =>
            l.toTraversable.map(EsModel.intParser).toSet
        },
        showcaseVoidFiller = Option(jmap get SHOWCASE_VOID_FILLER_ESFN).map(stringParser)
      )
  }
}

/**
 * Контейнер конфига, должен быть immutable целиком!
 * @param withBlocks Множество дополнительных id блоков, которые доступны этому узлу.
 * @param showcaseVoidFiller id или какие-то иные данные заполнятеля пустот в выдаче.
 */
case class NodeConf(
  // var использовать нельзя, т.к. это в целом плохо + дефолтовый конфиг сейчас шарится между инстансами узлов.
  withBlocks            : Set[Int] = Set.empty,
  showcaseVoidFiller    : Option[String] = None
) {
  import NodeConf._

  def nonEmpty = productIterator.exists {
    case tr: Traversable[_] => tr.nonEmpty
    case opt: Option[_]     => opt.nonEmpty
    case _ => true
  }
  def isEmpty = !nonEmpty

  def toPlayJson: JsObject = {
    var acc: FieldsJsonAcc = Nil
    if (withBlocks.nonEmpty)
      acc ::= WITH_BLOCKS_ESFN -> JsArray( withBlocks.toSeq.map(JsNumber(_)) )
    if (showcaseVoidFiller.isDefined)
      acc ::= SHOWCASE_VOID_FILLER_ESFN -> JsString(showcaseVoidFiller.get)
    JsObject(acc)
  }
}

