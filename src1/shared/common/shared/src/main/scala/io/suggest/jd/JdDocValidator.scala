package io.suggest.jd

import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths, MBlockExpandMode}
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.err.ErrorConstants
import io.suggest.jd.tags.{JdTag, MJdShadow, MJdTagNames, MJdtProps1}
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.img.MImgFmt
import io.suggest.jd.tags.qd._
import io.suggest.math.MathConst
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.scalaz.ScalazUtil
import io.suggest.scalaz.ZTreeUtil._
import japgolly.univeq._
import scalaz._
import scalaz.std.stream._
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.10.17 14:55
  * Description: Валидатор для целого документа.
  * Проверка дерева документа [[JdTag]] -- комплексная вещь, которую лучше вынести за пределы модели.
  * Конструктор содержит связанные с шаблоном данные для рендера.
  */
object JdDocValidator {

  private def QD      = "qd"
  private def BM      = "bm"
  private def PROPS1  = "props1"
  private def XY      = "xy"
  private def WIDTH   = "w"
  private def HEIGHT  = "h"
  private def EXPAND_MODE = "expandMode"
  private def BLUR    = "blur"
  private def STRIP   = "strip"
  private def S       = "s"
  private def STRIPS  = STRIP + S
  private def OP      = "op"
  private def OPS     = OP + S
  private def QD_OP   = QD + `.` + OP
  private def MAIN    = "main"
  private def ROTATE  = "rot"
  private def TEXT_SHADOW = "txSh"
  private def LINE_HEIGHT = "lineHeight"

}


class JdDocValidator(
                      tolerant      : Boolean,
                      edges         : Map[EdgeUid_t, MJdEdgeVldInfo],
                    ) {

  import JdDocValidator._
  import ErrorConstants.Words._


  implicit private class VldResExt[E, T]( val vldNel: ValidationNel[E, T] ) {

    /** Для внещне-управляемого восстановления после ошибки используется этот метод:
      * при ошибке - сброс ошибочного значения на указанное.
      * @param onError Значение, возвращаемое при любой ошибке.
      */
    def recoverTolerant(onError: => T): ValidationNel[E, T] = {
      if (tolerant && vldNel.isFailure) {
        Validation.success( onError )
      } else {
        vldNel
      }
    }

  }


  implicit private class VldOptResExt[E, T]( val vldOptNel: ValidationNel[E, Option[T]] ) {

    def recoverToNoneTolerant: ValidationNel[E, Option[T]] = {
      vldOptNel
        .recoverTolerant( None )
    }

  }


  /** Запуск валидации документа, представленного в виде дерева
    *
    * @param docTree Всё дерево документа.
    * @return Выверенное пересобранное дерево.
    */
  def validateDocumentTree(docTree: Tree[JdTag]): ValidationNel[String, Tree[JdTag]] = {
    docTree.validateNode(validateDocTag)(validateDocTags)
  }


  /** Провалидировать doc-тег.
    * @return Провалидировнный пересобранный document-тег.
    */
  private def validateDocTag(jdDoc: JdTag): ValidationNel[String, JdTag] = {
    def eDocPfx(suf: String) = ErrorConstants.EMSG_CODE_PREFIX + "doc" + `.` + suf
    (
      Validation.liftNel( jdDoc.name )( _ !=* MJdTagNames.DOCUMENT, eDocPfx( EXPECTED ) ) |@|
      Validation.liftNel( jdDoc.props1 )( _.nonEmpty, eDocPfx(PROPS1) )
        .recoverTolerant( MJdtProps1.empty ) |@|
      ScalazUtil.liftNelNone( jdDoc.qdProps, eDocPfx(QD) )
        .recoverToNoneTolerant
    ) { JdTag.apply }
  }


  /** Провалидировать список стрипов. */
  private def validateDocTags(jdts: Stream[Tree[JdTag]]): ValidationNel[String, Stream[Tree[JdTag]]] = {
    // На втором уровне могут быть только стрипы, хотя бы один должен там быть.
    def eStripsPfx(suf: String) = ErrorConstants.EMSG_CODE_PREFIX + STRIPS + `.` + suf

    Validation.liftNel(jdts.isEmpty)( identity, eStripsPfx( MISSING ))
      .andThen { _ =>
        val maxBlkCount = JdConst.MAX_STRIPS_COUNT
        Validation.liftNel(jdts)( _.lengthIs > maxBlkCount, eStripsPfx( TOO_MANY ))
          .recoverTolerant {
            jdts.take(maxBlkCount)
          }
      }
      .andThen { jdts2 =>
        ScalazUtil.validateAll(jdts2)( validateDocChildTree )
      }
  }


  /** Провалидировать один (каждый) стрип.
    * @return Возвращает Stream с максимум одним тегом-результатом.
    *         Возможно, в будущем будет какое-то удаление ненужного/некорректного стрипа,
    *         благодаря Stream это будет легко.
    */
  private def validateDocChildTree(jdTree: Tree[JdTag]): ValidationNel[String, Stream[Tree[JdTag]]] = {
    ScalazUtil.someValidationOrFail(BM) {
      val jdt = jdTree.rootLabel
      jdt.name match {
        case MJdTagNames.STRIP =>
          for (bm <- jdTree.rootLabel.props1.bm) yield {
            jdTree
              .validateNode(validateStrip)(validateContents(_, bm))
              .map { _ #:: Stream.empty }
          }
        case MJdTagNames.QD_CONTENT =>
          Some( validateQdTree(jdTree, contSz = None) )
        case _ =>
          None
      }
    }
  }

  /** Валидация тега стрипа. */
  private def validateStrip(stripJdt: JdTag): ValidationNel[String, JdTag] = {
    val eStripPfx = ErrorConstants.emsgF(STRIP)
    (
      Validation.liftNel(stripJdt.name)( _ !=* MJdTagNames.STRIP, eStripPfx( EXPECTED ) ) |@|
      validateStripProps1(stripJdt.props1) |@|
      ScalazUtil.liftNelNone( stripJdt.qdProps, eStripPfx(QD + `.` + UNEXPECTED) )
        .recoverToNoneTolerant
    )( JdTag.apply )
  }


  /** Провалидировать strip-пропертисы одного стрипа.
    *
    * @param props1 Инстас [[MJdtProps1]], извлечённый из strip-тега.
    * @return
    */
  private def validateStripProps1(props1: MJdtProps1): ValidationNel[String, MJdtProps1] = {
    val errMsgF = ErrorConstants.emsgF(STRIP + `.` + PROPS1)
    (
      ScalazUtil.liftNelOpt(props1.bgColor)( MColorData.validateHexCodeOnly )
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelOpt(props1.bgImg) { jdId =>
        MJdEdgeId.validate(jdId, edges,
          // TODO imgContSz=70x70 - Тут костыль, чтобы не было ошибок, если блок слишком большой для картинки по одной из сторон. Т.е. когда высота картинки 400px, а блок - 620px, чтобы не было ошибки.
          imgContSzOpt = OptionUtil.maybe(props1.widthPx.nonEmpty && props1.heightPx.nonEmpty)(MSize2di(70, 70))
        )
      }
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelNone(props1.topLeft, errMsgF( XY + `.` + UNEXPECTED))
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelOpt( props1.isMain ) {
        Validation.liftNel(_)(!_, errMsgF(`MAIN` + `.` + INVALID))
      }
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelNone(props1.rotateDeg, errMsgF(ROTATE + `.` + UNEXPECTED))
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelNone(props1.textShadow, errMsgF(TEXT_SHADOW + `.` + UNEXPECTED))
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelSome(props1.widthPx, errMsgF(WIDTH + `.` + MISSING)) { widthPx =>
        Validation.liftNel( widthPx )( BlockWidths.withValueOpt(_).isEmpty, errMsgF(WIDTH + `.` + INVALID) )
      } |@|
      ScalazUtil.liftNelSome(props1.heightPx, errMsgF(HEIGHT + `.` + MISSING)) { heightPx =>
        Validation.liftNel( heightPx )( BlockHeights.withValueOpt(_).isEmpty, errMsgF(WIDTH + `.` + INVALID) )
      } |@|
      ScalazUtil.liftNelOpt[String, MBlockExpandMode](props1.expandMode)( Validation.success )
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelNone(props1.lineHeight, errMsgF(LINE_HEIGHT))
        .recoverToNoneTolerant
    )( MJdtProps1.apply )
  }

  /** Провалидировать содержимое одного стрипа.
    * В содержимом могут быть qd-теги.
    *
    * @param contents Содержимое.
    */
  private def validateContents(contents: Stream[Tree[JdTag]], bm: BlockMeta): ValidationNel[String, Stream[Tree[JdTag]]] = {
    val maxElemsCount = JdConst.MAX_ELEMENTS_PER_MIN_BLOCK * bm.h.relSz * bm.w.relSz
    Validation.liftNel(contents)( _.lengthIs > maxElemsCount, TOO_MANY )
      .recoverTolerant { contents.take(maxElemsCount) }
      .andThen { contents2 =>
        ScalazUtil.validateAll(contents2) { validateQdTree(_, Some(bm)) }
      }
  }


  /** Валидация дерева одного qd-тега.
    * QD-тег является тегом-контейнером для qd-операций, т.е. qd-поля в нём пусты,
    * но можно задавать цвет фона.
    *
    * Для qd-тегов местами используется нестрогая валидация, т.е. теги могут срезаться (т.е. Stream.empty на выходе).
    *
    * @param qdTree Дерево qd-тега.
    * @param contSz Размер внешнего контейнера.
    * @return Список с максимум одним qd-тегом.
    */
  private def validateQdTree(qdTree: Tree[JdTag], contSz: Option[ISize2di]): ValidationNel[String, Stream[Tree[JdTag]]] = {
    validateQdTag(qdTree.rootLabel, contSz)
      .filter(_ => qdTree.subForest.nonEmpty)
      .fold {
        Validation.success(Stream.empty): ValidationNel[String, Stream[Tree[JdTag]]]
      } { qdTagValidationRes =>
        (
          qdTagValidationRes |@|
          validateQdTagContents( qdTree.subForest )
        ) { (jdt, nodes) =>
          Tree.Node(jdt, nodes) #:: Stream.empty
        }
      }
  }


  /** Валидация тега qd-контейнера.
    *
    * @param qdJdt Тег qd-content.
    * @param contSz Размер контейнера (блока).
    * @return Опциональный результат валидации.
    */
  private def validateQdTag(qdJdt: JdTag, contSz: Option[ISize2di]): Option[ValidationNel[String, JdTag]] = {
    val errMsgF = ErrorConstants.emsgF(QD + `.` + "cont")

    val validationRes = (
      Validation.liftNel(qdJdt.name)( _ !=* MJdTagNames.QD_CONTENT, errMsgF( EXPECTED ) ) |@|
      validateQdTagProps1( qdJdt.props1, contSz ) |@|
      ScalazUtil.liftNelNone( qdJdt.qdProps, errMsgF(QD) )
    )( JdTag.apply )

    // При ошибках -- возвращаем None.
    Some( validationRes )
      //.toOption
      //.map( Validation.success )
  }


  /** Валидация props1 для qd-контейнера.
    *
    * @param qdProps1 инстанс props1.
    * @param contSzOpt Размер внешнего контейнера, за пределы которого не должен вылезать контент.
    *                  Если None, то это qd-blockless (внеблоковый контент).
    */
  private def validateQdTagProps1(qdProps1: MJdtProps1, contSzOpt: Option[ISize2di]): ValidationNel[String, MJdtProps1] = {
    val errMsgF = ErrorConstants.emsgF(QD + `.` + PROPS1)
    // Для qd-тега допустимы цвет фона, и обязательна координата top-left. Остальное - дропаем.
    (
      ScalazUtil.liftNelOpt (qdProps1.bgColor)( MColorData.validateHexCodeOnly )
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelNone(qdProps1.bgImg, errMsgF("bgImg") )
        .recoverToNoneTolerant |@|
      contSzOpt.fold [ValidationNel[String, Option[MCoords2di]]] {
        // qd-blockless
        ScalazUtil.liftNelNone(qdProps1.topLeft, errMsgF(XY + `.` + UNEXPECTED))
          .recoverToNoneTolerant
      } { contSz =>
        // контент внутри блока
        ScalazUtil.liftNelSome(qdProps1.topLeft, errMsgF(XY + `.` + MISSING))(
          validateQdTagXY(_, contSz, qdProps1.rotateDeg)
        )
      } |@|
      ScalazUtil.liftNelNone(qdProps1.isMain, errMsgF(MAIN + `.` + UNEXPECTED))
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelOpt(qdProps1.rotateDeg.filter(_ !=* 0)) { rotateDeg =>
        MathConst.Counts.validateMinMax(
          v   = rotateDeg,
          min = -JdConst.ROTATE_MAX_ABS,
          max = JdConst.ROTATE_MAX_ABS,
          errMsgF(ROTATE)
        )
      }
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelOpt(qdProps1.textShadow)( validateQdTextShadow )
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelOpt(qdProps1.widthPx) { widthPx =>
        MathConst.Counts.validateMinMax(
          v = widthPx,
          min = JdConst.ContentWidth.MIN_PX,
          max = JdConst.ContentWidth.MAX_PX,
          errMsgF(WIDTH)
        )
      }
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelNone(qdProps1.heightPx, errMsgF(HEIGHT + `.` + UNEXPECTED))
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelNone(qdProps1.expandMode, errMsgF(EXPAND_MODE + `.` + UNEXPECTED))
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelOpt( qdProps1.lineHeight ) { lineHeight =>
        Validation.liftNel( lineHeight )( !MJdtProps1.LineHeight.isValid(_) , errMsgF(LINE_HEIGHT))
      }
        .recoverToNoneTolerant
    )( MJdtProps1.apply )
  }

  /** Валидация настроек тени. */
  private def validateQdTextShadow(shadow: MJdShadow): ValidationNel[String, MJdShadow] = {
    val C = JdConst.Shadow.TextShadow
    val errMsgF = ErrorConstants.emsgF(PROPS1 + `.` + TEXT_SHADOW)
    (
      MathConst.Counts.validateMinMax(
        v   = shadow.hOffset,
        min = -C.HORIZ_OFFSET_MIN_MAX,
        max = C.HORIZ_OFFSET_MIN_MAX,
        errMsgF(WIDTH)
      ) |@|
      MathConst.Counts.validateMinMax(
        v   = shadow.vOffset,
        min = -C.VERT_OFFSET_MIN_MAX,
        max = C.VERT_OFFSET_MIN_MAX,
        errMsgF(HEIGHT)
      ) |@|
      ScalazUtil.liftNelOpt( shadow.color ) { MColorData.validateHexCodeOnly } |@|
      ScalazUtil.liftNelOpt( shadow.blur ) { blur =>
        MathConst.Counts.validateMinMax(
          v   = blur,
          min = 0,
          max = C.BLUR_MAX * C.BLUR_FRAC,
          errMsgF(BLUR)
        )
      }
    )( MJdShadow.apply )
  }

  /** Валидация значения координаты qd-тега.
    *
    * @param coords top-left координата qd-тега.
    * @param contSz Размер контейнера.
    * @return Результат валидации.
    */
  private def validateQdTagXY(coords: MCoords2di, contSz: ISize2di, rotateDegOpt: Option[Int]): ValidationNel[String, MCoords2di] = {
    val errMsgF = ErrorConstants.emsgF(XY)
    val coords2size = coords.toSize
    // Функция-валидатор для каждой из координат.
    def __validateSide(e: String, f: ISize2di => Int) = {
      val contSzSide = f(contSz)
      // Повёрнутые элементы могут быть за экраном по xy, но на экране визуально.
      val sideAbsSz = contSzSide * rotateDegOpt.fold(2)(_ => 6)
      MathConst.Counts.validateMinMax(
        v = f(coords2size),
        // TODO Использовать sin/cos угла для рассчёта предельного значения?
        min = -sideAbsSz,
        max = sideAbsSz,
        eMsgPrefix = errMsgF(e)
      )
    }
    // Объединение валидаторов X и Y полей.
    (
      __validateSide("x", _.width) |@|
      __validateSide("y", _.height)
    )(MCoords2di.apply)
  }


  /** Валидация содержимого контейнера QD-контента.
    *
    * @param qdOps Список qd-op-тегов.
    * @return
    */
  private def validateQdTagContents(qdOps: Stream[Tree[JdTag]]): ValidationNel[String, Stream[Tree[JdTag]]] = {
    val qdOpsMax = JdConst.MAX_QD_OPS_COUNT
    Validation.liftNel(qdOps)(_.lengthIs > qdOpsMax, QD + `.` + OPS + `.` + TOO_MANY )
      .recoverTolerant { qdOps.take(qdOpsMax) }
      .andThen { ScalazUtil.validateAll(_)(validateQdOpTree) }
  }

  /** Валидация одного дерева тега одной qd-операции.
    *
    * @param qdOpTree Leaf-дерево тега одной qd-операции.
    * @return Результат операции.
    *         Stream позволяет отсеивать какие-нибудь операции во время работы.
    */
  private def validateQdOpTree(qdOpTree: Tree[JdTag]): ValidationNel[String, Stream[Tree[JdTag]]] = {
    val errMsgF = ErrorConstants.emsgF( QD + `.` + OP )
    qdOpTree
      .validateLeaf( errMsgF("chs" + `.` + UNEXPECTED) )( validateQdOp )
      .map(Stream(_))
  }


  /** Валидация одной qd-операции, описанной тегом.
    *
    * @param qdOp Тег с данными одной qd-операции.
    * @return Результат валидации.
    */
  private def validateQdOp(qdOp: JdTag): ValidationNel[String, JdTag] = {
    val errMsgF = ErrorConstants.emsgF(QD_OP)
    (
      Validation.liftNel( qdOp.name )( _ !=* MJdTagNames.QD_OP, errMsgF( EXPECTED ) ) |@|
      ScalazUtil.liftNelEmpty( qdOp.props1, errMsgF(PROPS1 + `.` + UNEXPECTED) )
        .recoverTolerant( MJdtProps1.empty ) |@|
      ScalazUtil.liftNelSome( qdOp.qdProps, errMsgF( QD + `.` + EXPECTED) )( validateQdOpProps )
    ) { JdTag.apply }
  }


  /** Валидация данных одной qd-операции.
    *
    * @param qdOpProps
    * @return
    */
  private def validateQdOpProps(qdOpProps: MQdOp): ValidationNel[String, MQdOp] = {
    val errMsgF = ErrorConstants.emsgF(QD_OP)
    val edgeInfoOpt = qdOpProps.edgeInfo
      .flatMap(ei => edges.get(ei.edgeUid))

    // Далее, в зависимости от данных эджа, валидация разветвляется:
    (
      Validation.liftNel(qdOpProps.opType)(_ !=* MQdOpTypes.Insert, errMsgF("type")) |@|
      ScalazUtil.liftNelOpt(qdOpProps.edgeInfo)( validateQdEdgeId(_, edgeInfoOpt) )
        .recoverToNoneTolerant |@|
      ScalazUtil.liftNelNone(qdOpProps.index, errMsgF("index" + `.` + UNEXPECTED))
        .recoverToNoneTolerant |@|
      validateQdAttrsText(qdOpProps.attrsText, edgeInfoOpt)
        .recoverToNoneTolerant |@|
      validateQdAttrsLine(qdOpProps.attrsLine, edgeInfoOpt)
        .recoverToNoneTolerant |@|
      validateQdAttrsEmbed(qdOpProps.attrsEmbed, edgeInfoOpt)
        .recoverToNoneTolerant
    ){ MQdOp.apply }
  }


  /** Валидация id эджа. */
  private def validateQdEdgeId(ei: MJdEdgeId, edgeOpt: Option[MJdEdgeVldInfo]): ValidationNel[String, MJdEdgeId] = {
    val errMsgF = ErrorConstants.emsgF( "edge" )
    (
      Validation.liftNel(ei.edgeUid)({ _ => edgeOpt.isEmpty }, errMsgF("id" + `.` + INVALID)) |@|
      // Перенести данные формата из эджа.
      // TODO Это наверное не правильно - управлять форматом на уровне валидации. Надо унести это куда?
      ScalazUtil.liftNelOpt[String, MImgFmt](
        edgeOpt
          .flatMap(_.img)
          .flatMap(_.dynFmt)
      )(Validation.success)
        .recoverToNoneTolerant |@|
      // Кроп для qd-эджей сейчас не поддерживается в интерфейсе редактора. Когда будет работать - надо будет заменить эту часть.
      ScalazUtil.liftNelNone(ei.crop, errMsgF(""))
        .recoverToNoneTolerant
    )( MJdEdgeId.apply )
  }


  /** Валидация текстовых аттрибутов в контексте возможного эджа.
    *
    * @param attrsTextOpt Текстовые аттрибуты, если есть.
    * @param edgeOpt Данные по связанному эджу, если такой эдж имеет место быть.
    * @return Результат валидации с почищенным инстансом.
    */
  private def validateQdAttrsText(attrsTextOpt: Option[MQdAttrsText],
                                  edgeOpt: Option[MJdEdgeVldInfo]): ValidationNel[String, Option[MQdAttrsText]] = {
    //val errMsgF = ErrorConstants.emsgF("text")
    edgeOpt.fold [ValidationNel[String, Option[MQdAttrsText]]] {
      // Нет контента -- нет и аттрибутов его оформления.
      Validation.success( None )
    } { _ =>
      // Есть контент. Значит аттрибуты оформления применимы.
      ScalazUtil.liftNelOpt( attrsTextOpt.filter(_.nonEmpty) )( MQdAttrsText.validateForStore )
        .recoverToNoneTolerant
    }
  }


  /** Проверка аттрибутов абзаца.
    *
    * @param attrsLineOpt Аттрибутика абзаца, если есть.
    * @param edgeOpt Данные по текущему эджу.
    * @return Провалидированный инстанс аттрибутов абзаца/строки.
    */
  private def validateQdAttrsLine(attrsLineOpt: Option[MQdAttrsLine], edgeOpt: Option[MJdEdgeVldInfo]): ValidationNel[String, Option[MQdAttrsLine]] = {
    // TODO Уточнить, возможно ли аттрибуты строки скрещивать с эджем. И если да, то с каким.
    val attrsLineOpt2 = attrsLineOpt.filter(_.nonEmpty)
    ScalazUtil.liftNelOpt( attrsLineOpt2 )( MQdAttrsLine.validateForStore )
      .recoverToNoneTolerant
  }


  /** Валидация опциональных embed-аттрибутов.
    *
    * @param attrsEmbedOpt Опциональный инстанс embed attrs.
    * @param edgeOpt Данные связанного эджа, если есть.
    * @return Провалидированный почищенный опциональный инстанс.
    */
  private def validateQdAttrsEmbed(attrsEmbedOpt: Option[MQdAttrsEmbed], edgeOpt: Option[MJdEdgeVldInfo]): ValidationNel[String, Option[MQdAttrsEmbed]] = {
    edgeOpt
      .filter { edge =>
        val P = MPredicates.JdContent
        (P.Frame :: P.Image :: Nil)
          .contains( edge.jdEdge.predicate )
      }
      .fold [ValidationNel[String, Option[MQdAttrsEmbed]]] {
        Validation.success( None )
      } { _ =>
        ScalazUtil.liftNelOpt( attrsEmbedOpt.filter(_.nonEmpty) )( MQdAttrsEmbed.validateForStore )
          .recoverToNoneTolerant
      }
  }

}
