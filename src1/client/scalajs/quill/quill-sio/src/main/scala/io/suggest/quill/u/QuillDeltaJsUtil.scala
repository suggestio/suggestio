package io.suggest.quill.u

import com.quilljs.delta.{DeltaInsertData_t, _}
import io.suggest.common.empty.OptionUtil
import io.suggest.font.{MFontSizes, MFonts}
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.IDocTag
import io.suggest.jd.tags.qd._
import io.suggest.js.JsTypes
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.primo.id.IId
import io.suggest.primo.{ISetUnset, SetVal, UnSetVal}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.text.MTextAligns

import scala.annotation.tailrec
import scala.scalajs.js
import scala.scalajs.js.{JSON, UndefOr, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 18:11
  * Description: Утиль для работы с quill delta.
  */
class QuillDeltaJsUtil extends Log {

  def purgeUnusedEdges(tpl: IDocTag, edgesMap: Map[EdgeUid_t, MJdEditEdge]): Map[EdgeUid_t, MJdEditEdge] = {
    val usedEdgeIds = tpl.deepEdgesUidsIter.toSet
    edgesMap.filterKeys(usedEdgeIds.contains)
  }

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
          val eOpt = qdOp.edgeInfo
            .flatMap { qdEi => edges.get( qdEi.edgeUid ) }
          val resOpt = for {
            e    <- eOpt
            data <- {
              e.predicate match {
                case MPredicates.JdContent.Text =>
                  e.text
                    .map(t => t: DeltaInsertData_t)
                case _ =>
                  val jsObj = js.Object().asInstanceOf[DeltaEmbed]
                  e.predicate match {
                    case MPredicates.JdContent.Image =>
                      for ( imgSrc <- e.url.orElse(e.text) ) yield {
                        jsObj.image = imgSrc
                        jsObj: DeltaInsertData_t
                      }
                    case MPredicates.JdContent.Video =>
                      for (videoUrl <- e.url) yield {
                        jsObj.video = videoUrl
                        jsObj: DeltaInsertData_t
                      }
                  }
              }
            }
          } yield {
            delta.insert( data, qdAttrsOpt2deltaAttrs(qdOp) )
          }
          if (resOpt.isEmpty)
            LOG.warn( ErrorMsgs.INSERT_PAYLOAD_EXPECTED, msg = List(qdOp.edgeInfo, eOpt) )
          delta

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
      blockQuote  = _val2s( attrs.blockquote ),
      align       = for ( quillAlignSU <- _string2s( attrs.align )) yield {
        quillAlignSU.map( MTextAligns.withQuillName(_).get )
      }
    )
    qdAttrs.optional
  }

  private def _attrToIntFromDirty( av: UndefOr[String | Int] ): Option[ISetUnset[Int]] = {
    for (valueOrNull <- av.toOption) yield {
      js.typeOf( valueOrNull.asInstanceOf[js.Any] ) match {
        case JsTypes.STRING => SetVal( valueOrNull.asInstanceOf[String].toInt )
        case JsTypes.NUMBER => SetVal( valueOrNull.asInstanceOf[Int] )
        case _              => UnSetVal
      }
    }
  }

  def deltaAttrs2qdAttrsEmbed(attrs: DeltaOpAttrs): Option[MQdAttrsEmbed] = {
    val qdAttrs = MQdAttrsEmbed(
      width  = _attrToIntFromDirty( attrs.width ),
      height = _attrToIntFromDirty( attrs.height )
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

    for (attrsEmbed <- qdOp.attrsEmbed)
      qdAttrsEmbedIntoDeltaAttrs( attrsEmbed, doa )

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


  private def qdAttrsTextIntoDeltaAttrs(qdAttrs: MQdAttrsText, attrs0: DeltaOpAttrs): Unit = {
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


  private def qdAttrsLineIntoDeltaAttrs(qdAttrsLine: MQdAttrsLine, attrs0: DeltaOpAttrs): Unit = {
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
    for (alignSU <- qdAttrsLine.align; align <- alignSU; quillAlignName <- align.quillName)
      attrs0.align = quillAlignName
  }


  private def qdAttrsEmbedIntoDeltaAttrs(qdAttrsEmbed: MQdAttrsEmbed, attrs0: DeltaOpAttrs): Unit = {
    for (widthSU <- qdAttrsEmbed.width)
      attrs0.width = js.defined { setUnsetOrNullRef( widthSU.map(_.toString) ) }
    for (heightSu <- qdAttrsEmbed.height)
      attrs0.height = js.defined { setUnsetOrNullRef( heightSu.map(_.toString) ) }
  }


  /** Конвертация дельты из quill-редактора в jd Text и обновлённую карту эджей.
    *
    * @param d Исходная дельта.
    * @param edges0 Исходная карта эджей.
    * @return Инстанс Text и обновлённая карта эджей.
    */
  def delta2qdTag(d: Delta, qdTag0: QdTag, edges0: Map[EdgeUid_t, MJdEditEdge]): (QdTag, Map[EdgeUid_t, MJdEditEdge]) = {

    val jdContPred = MPredicates.JdContent

    // Собрать id любых старых эджей текущего тега
    val oldEdgeIds = qdTag0
      .deepEdgesUidsIter
      .toSet

    // Отсеять все контент-эджи, они более неактуальны.
    val edgesNoJdCont = edges0.filterNot { case (_, e) =>
      (e.predicate ==>> jdContPred) &&
        (oldEdgeIds contains e.id)
    }

    // Множество edge id, которые уже заняты.
    val busyEdgeIds = edgesNoJdCont.keySet

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
    val newContEdgesMap = scala.collection.mutable.HashMap[String, MJdEditEdge]()

    // Пройтись по delta-операциям:
    val qdOps = d.ops
      .toIterator
      .map { dOp =>
        val deltaAttrsOpt = dOp.attributes.toOption
        MQdOp(
          opType = deltaOp2qdType( dOp ),
          edgeInfo = for (raw <- dOp.insert.toOption) yield {
            val typeOfRaw = js.typeOf(raw)

            // Проанализировать тип значения insert-поля.
            val jdEdge = if (typeOfRaw == JsTypes.STRING) {
              val text = raw.asInstanceOf[String]
              newContEdgesMap.getOrElseUpdate(text, {
                MJdEditEdge(
                  predicate = jdContPred.Text,
                  id        = __nextEdgeUid(),
                  text      = Some(text)
                )
              })

            } else if (typeOfRaw == JsTypes.OBJECT) {
              // TODO Дедублицировать с JsTypes.STRING ветвью: часть кода очень похожа, хотя отличий тоже хватает.
              val deltaEmbed = raw.asInstanceOf[DeltaEmbed]
              // Внутри или image, или video
              val pred = if (deltaEmbed.image.nonEmpty) {
                jdContPred.Image
              } else if (deltaEmbed.video.nonEmpty) {
                jdContPred.Video
              } else {
                LOG.error( ErrorMsgs.EMBEDDABLE_MEDIA_INFO_EXPECTED, msg = JSON.stringify(deltaEmbed) )
                throw new IllegalArgumentException(ErrorMsgs.EMBEDDABLE_MEDIA_INFO_EXPECTED)
              }

              val anyStrContent = deltaEmbed.image
                .orElse( deltaEmbed.video )
                .get

              newContEdgesMap.getOrElseUpdate(anyStrContent, {
                // Если инлайновая картинка, то тут будет Some()
                val inlineImageOpt = deltaEmbed.image
                  .toOption
                  .filter( _.startsWith("data:image/") )

                // Собрать embed edge
                MJdEditEdge(
                  predicate = pred,
                  id        = __nextEdgeUid(),
                  text      = inlineImageOpt,
                  url       = OptionUtil.maybe(inlineImageOpt.isEmpty)(anyStrContent)
                  // Файловые значения для whOpt не ставим в эдж, потому что мы тут не знаем их. Их выставляет сервер.
                )
              })
            } else {
              throw new IllegalArgumentException("op.i=" + raw)
            }

            MQdEdgeInfo(
              edgeUid = jdEdge.id
            )
          },
          index = dOp.delete
            .toOption
            .orElse( dOp.retain.toOption ),
          // TODO Opt: Можно выборочно грузить аттрибуты в зависимости от результатов предыдущих шагов.
          attrsText = deltaAttrsOpt
            .flatMap( deltaAtts2qdAttrs ),
          attrsLine = deltaAttrsOpt
            .flatMap( deltaAttrs2qdAttrsLine ),
          attrsEmbed = deltaAttrsOpt
            .flatMap( deltaAttrs2qdAttrsEmbed )
        )
      }
      .toList

    // Собрать и вернуть результаты исполнения.
    val tag = qdTag0.withOps( qdOps )
    val edges2 = edgesNoJdCont ++ IId.els2idMapIter[EdgeUid_t, MJdEditEdge]( newContEdgesMap.valuesIterator )

    (tag, edges2)
  }

}

