package io.suggest.quill.u

import com.quilljs.delta.{DeltaInsertData_t, _}
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.IDocTag
import io.suggest.jd.tags.qd._
import io.suggest.js.JsTypes
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.primo.id.IId
import io.suggest.primo.{ISetUnset, SetVal, UnSetVal}

import scala.annotation.tailrec
import scala.scalajs.js
import scala.scalajs.js.{JSON, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 18:11
  * Description: Утиль для работы с quill delta.
  */
class QuillDeltaJsUtil {

  /** Конверсия из QdTag в дельту, понятную quill-редактору.
    *
    * @param qd s.io-тег с quill-данными.
    * @param edges Карта эджей.
    * @return Инстанс js.native-дельты дял отправки в редактор.
    */
  def qdTag2delta(qd: QdTag, edges: Map[Int, MJdEditEdge]): Delta = {
    qd.ops.foldLeft( new Delta() ) { (delta, qdOp) =>
      qdOp.opType match {
        case MQdOpTypes.Insert =>
          val arg1: DeltaInsertData_t = qdOp.edgeInfo
            .flatMap { qdEi =>
              edges
                .get( qdEi.edgeUid )
                .flatMap(_.text)
            }
            .orElse {
              // TODO Реализовать поддержку IEmbed => Delta JSON
              ???
            }
            .get
          delta.insert( arg1, qdAttrsOpt2deltaAttrs(qdOp.attrs) )

        case MQdOpTypes.Delete =>
          delta.delete(
            howMany = qdOp.index.get
          )

        case MQdOpTypes.Retain =>
          delta.retain(
            length     = qdOp.index.get,
            attributes = qdAttrsOpt2deltaAttrs(qdOp.attrs)
          )
      }
    }
  }


  /** Вычисление scala-типа delta-операции. */
  def deltaOp2qdType(dOp: DeltaOp): MQdOpType = {
    if (dOp.insert.nonEmpty) {
      MQdOpTypes.Insert
    } else if (dOp.delete.nonEmpty) {
      MQdOpTypes.Delete
    } else if (dOp.retain.nonEmpty) {
      MQdOpTypes.Retain
    } else {
      throw new IllegalArgumentException("d.op: " + JSON.stringify(dOp))
    }
  }

  /** Конвертация undefined|"..."|null в scala. */
  private def _string2s(attrValue: js.UndefOr[String]): Option[ISetUnset[String]] = {
    for (colorOrNull <- attrValue.toOption) yield {
      Option( colorOrNull )
        .fold [ISetUnset[String]] ( UnSetVal ) { SetVal.apply }
    }
  }

  /** Конвертация undefined|"#ffeecc"|null в scala. */
  private def _color2s(attrValue: js.UndefOr[String]): Option[ISetUnset[MColorData]] = {
    _string2s(attrValue)
      .map( _.map(MColorData.stripingDiez) )
  }

  private def _val2s[T <: AnyVal](attrValue: js.UndefOr[T | Null]): Option[ISetUnset[T]] = {
    for (valueOrNull <- attrValue.toOption) yield {
      Option(valueOrNull)
        .fold[ISetUnset[T]](UnSetVal) { value =>
          SetVal( value.asInstanceOf[T] )
        }
    }
  }


  def deltaAtts2qdAttrs(attrs: DeltaOpAttrs): Option[MQdAttrs] = {
    val qdAttrs = MQdAttrs(
      bold      = _val2s( attrs.bold ),
      italic    = _val2s( attrs.italic ),
      underline = _val2s( attrs.underline ),
      color     = _color2s( attrs.color ),
      link      = _string2s( attrs.link ),
      header    = _val2s( attrs.header ),
      src       = _string2s( attrs.src )
    )
    if (qdAttrs.isEmpty)
      None
    else
      Some(qdAttrs)
  }

  def setUnsetOrNullVal[T <: AnyVal](su: ISetUnset[T]): T | Null = {
    su match {
      case SetVal(v) => v
      case UnSetVal  => null.asInstanceOf[T | Null]
    }
  }
  // TODO Дедублицировать код этих двух функций.
  def setUnsetOrNullRef[T <: AnyRef](su: ISetUnset[T]): T = {
    su match {
      case SetVal(v) => v
      case UnSetVal  => null.asInstanceOf[T]
    }
  }


  def qdAttrsOpt2deltaAttrs(qdAttrsOpt: Option[MQdAttrs]): js.UndefOr[DeltaOpAttrs] = {
    if (qdAttrsOpt.isEmpty) {
      js.undefined
    } else {
      qdAttrs2deltaAttrs( qdAttrsOpt.get )
    }
  }
  def qdAttrs2deltaAttrs(qdAttrs: MQdAttrs): js.UndefOr[DeltaOpAttrs] = {
    val doa = js.Object().asInstanceOf[DeltaOpAttrs]

    for (boldSU <- qdAttrs.bold)
      doa.bold = setUnsetOrNullVal( boldSU )
    for (italicSU <- qdAttrs.italic)
      doa.italic = setUnsetOrNullVal( italicSU )
    for (underlineSU <- qdAttrs.underline)
      doa.underline = setUnsetOrNullVal( underlineSU )
    for (colorSU <- qdAttrs.color)
      doa.color = setUnsetOrNullRef( colorSU.map(_.hexCode) )
    for (link <- qdAttrs.link)
      doa.link = js.defined( setUnsetOrNullRef( link ) )
    for (header <- qdAttrs.header)
      doa.header = setUnsetOrNullVal( header )
    for (src <- qdAttrs.src)
      doa.src = js.defined( setUnsetOrNullRef( src ) )

    // Вернуть результат, если в аккамуляторе есть хоть какие-то данные:
    if (doa.asInstanceOf[js.Dictionary[js.Any]].nonEmpty) {
      doa
    } else {
      js.undefined
    }
  }


  /** Конвертация дельты из quill-редактора в jd Text и обновлённую карту эджей.
    *
    * @param d Исходная дельта.
    * @param edges0 Исходная карта эджей.
    * @return Инстанс Text и обновлённая карта эджей.
    */
  def delta2qdTag(d: Delta, jdTag0: IDocTag, edges0: Map[Int, MJdEditEdge]): (QdTag, Map[Int, MJdEditEdge]) = {

    val textPred = MPredicates.Text

    // Собрать id любых старых эджей текущего тега
    val oldEdgeIds = jdTag0.deepIter
      .flatMap {
        case qd: QdTag => qd :: Nil
        case _ => Nil
      }
      .flatMap(_.ops)
      .flatMap(_.edgeInfo)
      .map(_.edgeUid)
      .toSet


    // Отсеять все текстовые эджи, они более не актуальны.
    // TODO XXX нужно дропать только то, что относится к текущему QdTag, а не всё сразу.
    val edgesNoText = edges0.filterNot { case (_, e) =>
      e.predicate == textPred && oldEdgeIds.contains(e.id)
    }

    // Множество edge id, которые уже заняты.
    val busyEdgeIds = edgesNoText.keySet

    var edgeUidCounter = 0

    @tailrec def __nextEdgeUid(): Int = {
      if (busyEdgeIds contains edgeUidCounter) {
        edgeUidCounter += 1
        __nextEdgeUid()
      } else {
        edgeUidCounter
      }
    }

    // Карта новых текстовых эджей.
    val newTextEdgesMap = scala.collection.mutable.HashMap[String, MJdEditEdge]()

    // Пройтись по delta-операциям:
    val qdOps = d.ops
      .toIterator
      .map { dOp =>
        MQdOp(
          opType = deltaOp2qdType( dOp ),
          edgeInfo = dOp.insert.toOption.flatMap { raw =>
            val typeOfRaw = js.typeOf(raw)
            if (typeOfRaw == JsTypes.STRING) {
              val text = raw.asInstanceOf[String]
              val te = newTextEdgesMap.getOrElse(text, {
                val textEdge = MJdEditEdge(
                  predicate = textPred,
                  id = __nextEdgeUid(),
                  text = Some(text)
                )
                newTextEdgesMap(text) = textEdge
                textEdge
              })
              val qdEdgeInfo = MQdEdgeInfo(
                edgeUid = te.id
              )
              Some(qdEdgeInfo)

            } else if (typeOfRaw == JsTypes.NUMBER) {
              // TODO Бывает, что какой-то embed-контент задан через embed type id (1 или другие числа какие-то, хз).
              val embedTypeId = typeOfRaw.asInstanceOf[Int]
              ???
            } else if (typeOfRaw == JsTypes.OBJECT) {
              // TODO Embed задан объектом. Это нормально.
              ???
            } else {
              throw new IllegalArgumentException("op.i=" + raw)
            }
          },
          extEmbed = None, // TODO Надо поискать в insert данные по внешнему video/image
          index = dOp.delete
            .toOption
            .orElse( dOp.retain.toOption ),
          attrs = dOp.attributes
            .toOption
            .flatMap( deltaAtts2qdAttrs )
        )
      }
      .toSeq

    // Собрать и вернуть результаты исполнения.
    val tag = QdTag( qdOps )
    val edges2 = edgesNoText ++ IId.els2idMapIter[Int, MJdEditEdge]( newTextEdgesMap.valuesIterator )

    (tag, edges2)
  }

}

