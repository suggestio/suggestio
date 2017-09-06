package io.suggest.quill.u

import com.quilljs.delta.{DeltaInsertData_t, _}
import io.suggest.font.{MFontSizes, MFonts}
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.IDocTag
import io.suggest.jd.tags.qd._
import io.suggest.js.JsTypes
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
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
  def qdTag2delta(qd: QdTag, edges: Map[EdgeUid_t, MJdEditEdge]): Delta = {
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
          delta.insert( arg1, qdAttrsOpt2deltaAttrs(qdOp) )

        case MQdOpTypes.Delete =>
          delta.delete(
            howMany = qdOp.index.get
          )

        case MQdOpTypes.Retain =>
          delta.retain(
            length     = qdOp.index.get,
            attributes = qdAttrsOpt2deltaAttrs(qdOp)
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

  private def _listType2s(attrValue: js.UndefOr[String]): Option[ISetUnset[MQdListType]] = {
    _string2s(attrValue)
      .map { _.map(MQdListTypes.withValue) }
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


  def deltaAtts2qdAttrs(attrs: DeltaOpAttrs): Option[MQdAttrsText] = {
    val qdAttrs = MQdAttrsText(
      bold        = _val2s( attrs.bold ),
      italic      = _val2s( attrs.italic ),
      underline   = _val2s( attrs.underline ),
      strike      = _val2s( attrs.strike ),
      color       = _color2s( attrs.color ),
      background  = _color2s( attrs.background ),
      link        = _string2s( attrs.link ),
      src         = _string2s( attrs.src ),
      font        = for (fontCssClassSu <- _string2s( attrs.font )) yield {
        for (fontCssClass <- fontCssClassSu) yield {
          // TODO legacy-enum, нельзя тут юзать withName(), т.к. у них разные множества ключей. Перейти на withCssClass(), когда MFonts будет Enumeratum-моделью.
          MFonts.maybeWithName( fontCssClass ).get
        }
      },
      size = for (sizeStrSU <- _string2s( attrs.size )) yield {
        for (sizeStr <- sizeStrSU) yield {
          val sizeInt = sizeStr.toInt
          MFontSizes.withValue( sizeInt )
        }
      },
      script      = for (scriptRawSU <- _string2s( attrs.script )) yield {
        for (scriptRaw <- scriptRawSU) yield {
          MQdScripts.withQuillNameOpt( scriptRaw ).get
        }
      }
    )
    qdAttrs.optional
  }


  def deltaAttrs2qdAttrsLine(attrs: DeltaOpAttrs): Option[MQdAttrsLine] = {
    val qdAttrs = MQdAttrsLine(
      header      = _val2s( attrs.header ),
      list        = _listType2s( attrs.list ),
      indent      = _val2s( attrs.indent ),
      codeBlock   = _val2s( attrs.`code-block` ),
      blockQuote  = _val2s( attrs.blockquote )
    )
    qdAttrs.optional
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


  def qdAttrsOpt2deltaAttrs(qdOp: MQdOp): js.UndefOr[DeltaOpAttrs] = {
    val doa = js.Object().asInstanceOf[DeltaOpAttrs]

    for (attrsText <- qdOp.attrsText)
      qdAttrsTextIntoDeltaAttrs( attrsText, doa )

    for (attrsLine <- qdOp.attrsLine)
      qdAttrsLineIntoDeltaAttrs( attrsLine, doa )

    // Вернуть результат, если в аккамуляторе есть хоть какие-то данные:
    if (doa.asInstanceOf[js.Dictionary[js.Any]].nonEmpty) {
      doa
    } else {
      js.undefined
    }
  }

  private def _colorSUToStringSU(mcdSU: ISetUnset[MColorData]): ISetUnset[String] = {
    mcdSU.map(_.hexCode)
  }


  def qdAttrsTextIntoDeltaAttrs(qdAttrs: MQdAttrsText, attrs0: DeltaOpAttrs): Unit = {
    for (boldSU <- qdAttrs.bold)
      attrs0.bold = setUnsetOrNullVal( boldSU )
    for (italicSU <- qdAttrs.italic)
      attrs0.italic = setUnsetOrNullVal( italicSU )
    for (underlineSU <- qdAttrs.underline)
      attrs0.underline = setUnsetOrNullVal( underlineSU )
    for (strikeSU <- qdAttrs.strike)
      attrs0.strike = setUnsetOrNullVal( strikeSU )
    for (colorSU <- qdAttrs.color)
      attrs0.color = js.defined( setUnsetOrNullRef( _colorSUToStringSU(colorSU) ) )
    for (backgroundSU <- qdAttrs.background)
      attrs0.background = js.defined( setUnsetOrNullRef( _colorSUToStringSU(backgroundSU) ) )
    for (link <- qdAttrs.link)
      attrs0.link = js.defined( setUnsetOrNullRef( link ) )
    for (src <- qdAttrs.src)
      attrs0.src = js.defined( setUnsetOrNullRef( src ) )
    // В delta-аттрибуты нужно передать css-класс шрифта, а не его id
    for (fontSU <- qdAttrs.font)
      attrs0.font = js.defined( setUnsetOrNullRef( fontSU.map(_.cssFontFamily) ) )
    for (sizeSU <- qdAttrs.size)
      attrs0.size = js.defined( setUnsetOrNullRef( sizeSU.map(_.value.toString) ) )
    for (scriptSU <- qdAttrs.script)
      attrs0.script = js.defined( setUnsetOrNullRef( scriptSU.map(_.quillName) ) )
  }


  def qdAttrsLineIntoDeltaAttrs(qdAttrsLine: MQdAttrsLine, attrs0: DeltaOpAttrs): Unit = {
    for (headerSU <- qdAttrsLine.header)
      attrs0.header = setUnsetOrNullVal( headerSU )
    for (listSU <- qdAttrsLine.list)
      attrs0.list = setUnsetOrNullRef( listSU.map(_.value) )
    for (indentSU <- qdAttrsLine.indent)
      attrs0.indent = setUnsetOrNullVal( indentSU )
    for (codeBlockSU <- qdAttrsLine.codeBlock)
      attrs0.`code-block` = setUnsetOrNullVal( codeBlockSU )
    for (blockQuoteSU <- qdAttrsLine.blockQuote)
      attrs0.blockquote = setUnsetOrNullVal( blockQuoteSU )
  }


  /** Конвертация дельты из quill-редактора в jd Text и обновлённую карту эджей.
    *
    * @param d Исходная дельта.
    * @param edges0 Исходная карта эджей.
    * @return Инстанс Text и обновлённая карта эджей.
    */
  def delta2qdTag(d: Delta, jdTag0: IDocTag, edges0: Map[EdgeUid_t, MJdEditEdge]): (QdTag, Map[EdgeUid_t, MJdEditEdge]) = {

    val textPred = MPredicates.Text

    // Собрать id любых старых эджей текущего тега
    val oldEdgeIds = jdTag0
      .deepOfTypeIter[QdTag]
      .flatMap( _.deepEdgesUidsIter )
      .toSet


    // Отсеять все текстовые эджи, они более не актуальны.
    // TODO XXX нужно дропать только то, что относится к текущему QdTag, а не всё сразу.
    val edgesNoText = edges0.filterNot { case (_, e) =>
      e.predicate == textPred && oldEdgeIds.contains(e.id)
    }

    // Множество edge id, которые уже заняты.
    val busyEdgeIds = edgesNoText.keySet

    // Переменная-счётчик для эджей во время цикла. Можно её запихать в аккамулятор и идти через foldLeft + List.reverse вместо map.
    var edgeUidCounter = if (busyEdgeIds.isEmpty) {
      -1
    } else {
      busyEdgeIds.max
    }

    // Получение незанятого id'шника для нового эджа.
    @tailrec def __nextEdgeUid(): EdgeUid_t = {
      edgeUidCounter += 1
      if (busyEdgeIds contains edgeUidCounter) {
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
        val deltaAttrsOpt = dOp.attributes.toOption
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
          attrsText = deltaAttrsOpt
            .flatMap( deltaAtts2qdAttrs ),
          attrsLine = deltaAttrsOpt
            .flatMap( deltaAttrs2qdAttrsLine )
        )
      }
      .toList

    // Собрать и вернуть результаты исполнения.
    val tag = QdTag(
      html = None,
      ops  = qdOps
    )
    val edges2 = edgesNoText ++ IId.els2idMapIter[EdgeUid_t, MJdEditEdge]( newTextEdgesMap.valuesIterator )

    (tag, edges2)
  }

}

