package io.suggest.quill.u

import com.quilljs.delta._
import io.suggest.color.MColorData
import io.suggest.common.html.HtmlConstants
import io.suggest.font.{MFontSizes, MFonts}
import io.suggest.jd.{MJdEdge, MJdEdgeId}
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import io.suggest.jd.tags.qd._
import io.suggest.js.JsTypes
import io.suggest.n2.edge.{EdgeUid_t, MEdgeDataJs, MEdgeDoc, MPredicate, MPredicates}
import io.suggest.primo.{ISetUnset, SetVal, UnSetVal}
import io.suggest.log.Log
import io.suggest.text.MTextAligns
import japgolly.univeq._
import io.suggest.msg.ErrorMsgs
import io.suggest.primo.id._

import scala.scalajs.js
import scala.scalajs.js.{JSON, UndefOr, |}
import scalaz.{EphemeralStream, Tree}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 18:11
  * Description: Утиль для работы с quill delta.
  */
class QuillDeltaJsUtil extends Log {

  /** Конверсия из QdTag в дельту, понятную quill-редактору.
    *
    * @param qd s.io-тег с quill-данными.
    * @param edges Карта эджей.
    * @return Инстанс js.native-дельты дял отправки в редактор.
    */
  def qdTag2delta(qd: Tree[JdTag], edges: Map[EdgeUid_t, MEdgeDataJs]): Delta = {
    qd.qdOps
      .foldLeft( new Delta() ) { (delta, qdOp) =>
        qdOp.opType match {
          case MQdOpTypes.Insert =>
            val eOpt = qdOp.edgeInfo
              .flatMap { qdEi => edges.get( qdEi.edgeUid ) }
            val resOpt = for {
              e    <- eOpt
              data <- {
                e.jdEdge.predicate match {
                  case MPredicates.JdContent.Text =>
                    e.jdEdge.edgeDoc.text
                      .map(t => t: DeltaInsertData_t)
                  case _ =>
                    val jsObj = js.Object().asInstanceOf[DeltaEmbed]
                    e.jdEdge.predicate match {
                      case MPredicates.JdContent.Image =>
                        for ( imgSrc <- e.jdEdge.imgSrcOpt ) yield {
                          jsObj.image = imgSrc
                          jsObj: DeltaInsertData_t
                        }
                      case MPredicates.JdContent.Frame =>
                        for (videoUrl <- e.jdEdge.url) yield {
                          jsObj.video = videoUrl
                          jsObj: DeltaInsertData_t
                        }

                      case other =>
                        throw new UnsupportedOperationException( ErrorMsgs.UNSUPPORTED_VALUE_OF_ARGUMENT + HtmlConstants.SPACE + (other, e))
                    }
                }
              }
            } yield {
              delta.insert( data, qdAttrsOpt2deltaAttrs(qdOp) )
            }
            if (resOpt.isEmpty)
              logger.warn( ErrorMsgs.INSERT_PAYLOAD_EXPECTED, msg = List(qdOp.edgeInfo, eOpt) )
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
      .map( _.map { hexCode => MColorData( MColorData.stripDiez(hexCode) ) } )
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
          MFonts.withCssClass( fontCssClass )
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
    * @return Инстанс Text и ДОПОЛНЕННАЯ карта эджей.
    *         В выходной карте почти всегда есть никем неиспользуемые элементы:
    *         тут внутри метода нельзя определить, используется ли тот или иной эдж на самом деле.
    *         Надо чистить карту снаружи на основе всего документа через purgeUnusedEdges
    */
  def delta2qdTag(d: Delta, qdTag0: Tree[JdTag],
                  edges0: Map[EdgeUid_t, MEdgeDataJs]): (Tree[JdTag], Map[EdgeUid_t, MEdgeDataJs]) = {
    assert( qdTag0.rootLabel.name ==* MJdTagNames.QD_CONTENT )

    // Часто-используемый предикат берём в оборот...
    val jdContPred = MPredicates.JdContent

    // Множество edge uids, которые уже заняты.
    val busyEdgeIds = edges0.keySet

    // Карта новых текстовых эджей. Ключ -- это строка, например текст, URL или ещё что-то текстовое.
    val str2EdgeMap = {
      val b = scala.collection.mutable.HashMap.newBuilder[String, MEdgeDataJs]
      b ++= {
        // Некоторые эджи из исходной карты могут быть потеряны. Это не страшно: потом надо просто объеденить старую и новую эдж-карты.
        for {
          e   <- edges0.valuesIterator
          key <- (
            e.jdEdge.edgeDoc.text ::
            e.jdEdge.url ::
            //e.fileJs.flatMap(_.blobUrl) ::    // 2017.sep.28 Quill не поддерживает, игнорим.
            e.jdEdge.fileSrv.flatMap(_.url) ::
            Nil
          ).flatten
        } yield {
          key -> e
        }
      }
      b.result()
    }

    // Счётчик id эджей.
    var lastEdgeUid = if (busyEdgeIds.isEmpty) -1 else busyEdgeIds.max
    // Инкрементор и возвращатор счётчика id эджей.
    def nextEdgeUid(): Int = {
      lastEdgeUid += 1
      lastEdgeUid
    }

    // Пройтись по delta-операциям:
    val qdOps = d.ops
      .iterator
      .map { dOp =>
        val deltaAttrsOpt = dOp.attributes.toOption

        val jdEdgeJsOpt = for (raw <- dOp.insert.toOption) yield {
          val typeOfRaw = js.typeOf(raw)

          // Проанализировать тип значения insert-поля: там или строка текста, или object с данными embed'а.
          if (typeOfRaw ==* JsTypes.STRING) {
            val text = raw.asInstanceOf[String]
            str2EdgeMap.getOrElseUpdate(text, {
              MEdgeDataJs(
                jdEdge = MJdEdge(
                  predicate = jdContPred.Text,
                  edgeDoc = MEdgeDoc(
                    id        = Some( nextEdgeUid() ),
                    text      = Some( text ),
                  ),
                )
              )
            })

          } else if (typeOfRaw ==* JsTypes.OBJECT) {
            // TODO Дедублицировать с JsTypes.STRING ветвью: часть кода очень похожа, хотя отличий тоже хватает.
            val deltaEmbed = raw.asInstanceOf[DeltaEmbed]
            // Внутри или image, или video
            val pred = if (deltaEmbed.image.nonEmpty) {
              jdContPred.Image
            } else if (deltaEmbed.video.nonEmpty) {
              jdContPred.Frame
            } else {
              logger.error( ErrorMsgs.EMBEDDABLE_MEDIA_INFO_EXPECTED, msg = JSON.stringify(deltaEmbed) )
              throw new IllegalArgumentException(ErrorMsgs.EMBEDDABLE_MEDIA_INFO_EXPECTED)
            }

            val anyStrContent = deltaEmbed.image
              .orElse( deltaEmbed.video )
              .get

            str2EdgeMap.getOrElseUpdate(anyStrContent, {
              // Собрать embed edge
              MEdgeDataJs(
                jdEdge = MJdEdge(
                  predicate = pred,
                  edgeDoc = MEdgeDoc(
                    id = Some( nextEdgeUid() ),
                  ),
                  url = Some( anyStrContent ),
                  // Файловые значения для whOpt не ставим в эдж, потому что мы тут не знаем их. Их выставляет сервер.
                )
              )
            })
          } else {
            throw new IllegalArgumentException("op.i=" + raw)
          }
        }

        val qdOp = MQdOp(
          opType = deltaOp2qdType( dOp ),
          edgeInfo = jdEdgeJsOpt.map { jdEdgeJs =>
            MJdEdgeId(
              edgeUid = jdEdgeJs.jdEdge.edgeDoc.id.get,
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
          attrsEmbed = {
            val attrsEmbedOpt = deltaAttrsOpt
              .flatMap( deltaAttrs2qdAttrsEmbed )

            // Для frame - обязательны wh, для image - только w.
            val P = MPredicates.JdContent
            jdEdgeJsOpt
              .filter { e =>
                (P.Frame :: P.Image :: Nil) contains[MPredicate] e.jdEdge.predicate
              }
              .fold( attrsEmbedOpt ) { e =>
                // Требуется обязательный attrsEmbed.
                lazy val dfltSz = HtmlConstants.Iframes.whCsspxDflt
                val r = attrsEmbedOpt.fold {
                  MQdAttrsEmbed(
                    width  = Some( SetVal(dfltSz.width) ),
                    height = Option.when( e.jdEdge.predicate ==* P.Frame )( SetVal(dfltSz.height) ),
                  )
                } { attrsEmbed =>
                  // Уже заданы какие-то embed-аттрибуты. Подогнать под требования:
                  var changesAcc = List.empty[MQdAttrsEmbed => MQdAttrsEmbed]

                  // Для картинки и для фрейма - ширина обязательная.
                  if (attrsEmbed.width.isEmpty)
                    changesAcc ::= (MQdAttrsEmbed.width set Some(SetVal(dfltSz.width)))

                  // Для фрейма - высота обязательная.
                  if ((e.jdEdge.predicate ==* P.Frame) && attrsEmbed.height.isEmpty)
                    changesAcc ::= (MQdAttrsEmbed.height set Some(SetVal(dfltSz.height)))

                  changesAcc
                    .reduceOption(_ andThen _)
                    .fold( attrsEmbed )( _(attrsEmbed) )
                }
                Some(r)
              }
          }
        )
        // Завернуть в дерево.
        Tree.Leaf(
          JdTag.qdOp(qdOp)
        )
        // Вернуть аккамулятор, т.е. новый edgeUid.
      }
      // .toList, потому что нам требуется неленивый Stream.
      // Если ставить ленивый .toStream сразу, то выяснится, что delta является pure-js инстансом, который ПОВТОРНО используется quill'ом.
      .toIndexedSeq

    // Собрать и вернуть результаты исполнения.
    val tag = Tree.Node(
      root   = qdTag0.rootLabel,
      forest = EphemeralStream( qdOps: _* ),
    )

    // Объеденить старую и обновлённые эдж-карты.
    val edges2 = edges0 ++ str2EdgeMap
      .valuesIterator
      .zipWithIdIter[EdgeUid_t]

    (tag, edges2)
  }

}

