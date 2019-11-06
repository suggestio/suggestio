package io.suggest.jd.render.v

import com.github.dantrain.react.stonecutter._
import com.github.souporserious.react.measure.{ContentRect, Measure, MeasureChildrenArgs, MeasureProps}
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.css.Css
import io.suggest.grid.GridBuilderUtilJs
import io.suggest.jd.{MJdEdgeId, MJdTagId}
import io.suggest.jd.render.m._
import io.suggest.jd.tags._
import io.suggest.model.n2.edge.MPredicates
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import diode.data.PendingBase
import io.suggest.dev.MSzMult
import io.suggest.img.ImgUtilRJs
import io.suggest.sjs.common.log.Log
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.{TagOf, VdomElement}
import japgolly.univeq._
import org.scalajs.dom.html
import scalacss.ScalaCssReact._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 19:00
  * Description: Ядро react-рендерера JSON-документов.
  */

class JdR(
           jdCssStatic        : JdCssStatic,
         )
  extends Log
{ jdR =>

  type Props_t = MJdArgs
  type Props = ModelProxy[Props_t]


  /** Чтобы qd-blockless с ротацией не наезжал слоем на блоки, надо их развести по этажам плитки.
    * Вызывается при сборке CSSGrid children.
    *
    * @param jdt jd-тег.
    * @return Пустой или непустой TagMod.
    */
  def fixZIndexIfBlock(jdt: JdTag): TagMod =
    ReactCommonUtil.maybe( jdt.name ==* MJdTagNames.STRIP )( jdCssStatic.smBlockOuter )


  /** Для разделения edit и read-only режима используется этот трейт,
    * реализующий только базовый рендер с возможностью внутреннего расширения.
    */
  trait JdRrrBase {

    trait QdContentBase {

      def _qdContentRrrHtml(p: MJdRrrProps): VdomElement = {
        QdRrrHtml(
          jdCssStatic = jdCssStatic,
          rrrProps    = p,
        )
          .render()
      }

      def _renderQdContentTag(state: MJdRrrProps): TagOf[html.Element] = {
        import state.jdArgs.jdRuntime.jdCss

        val qdTag = state.subTree.rootLabel

        var contTagModsAcc = List[TagMod](
          _bgColorOpt( qdTag, state.jdArgs ),

          // Рендер qd-контента в html.
          _qdContentRrrHtml( state ),
        )

        // Поддержка абсолютного позиционирования внутри контейнера:
        val absPosStyl: TagMod = jdCss.absPosF( state.tagId )

        if (qdTag.props1.topLeft.nonEmpty) {
          // Обычный контент внутри блока.
          contTagModsAcc =
            // Поддержка перетаскивания внутри блока
            (jdCssStatic.absPosStyleAll: TagMod) ::
            absPosStyl ::
            contTagModsAcc
        } else {
          // Внеблоковый контент. Бывает, что высота уже измерена и не измерена. Отработать обе ситуации:
          contTagModsAcc ::= jdCssStatic.qdBl
          val qdBlOpt = state.jdArgs.jdRuntime.data.qdBlockLess.get( state.tagId )
          if ( qdBlOpt.exists(_.nonEmpty) )
            contTagModsAcc ::= absPosStyl
        }

        // Вращение, если активно:
        for (rotateDeg <- qdTag.props1.rotateDeg)
          contTagModsAcc ::= state.jdArgs.jdRuntime.jdCss.rotateF( rotateDeg )

        // CSS-класс принудительной ширины, если задан.
        if (qdTag.props1.widthPx.nonEmpty)
          contTagModsAcc ::= jdCss.contentWidthF( state.tagId )

        // Стиль для теней
        if (qdTag.props1.textShadow.nonEmpty)
          contTagModsAcc ::= jdCss.contentShadowF( state.tagId )

        // Общие стили для текстов внутри (line-height):
        for (lineHeightPx <- qdTag.props1.lineHeight)
          contTagModsAcc ::= jdCss.lineHeightF( lineHeightPx )

        <.div( contTagModsAcc : _* )
      }

      def _doRender(state: MJdRrrProps): TagOf[html.Element] = {
        // Если рендер ВНЕ блока, то нужно незаметно измерить высоту блока.
        val tag0 = _renderQdContentTag(state)
        if (
          state.parent
            .exists(_.name ==* MJdTagNames.STRIP)
        ) {
          // Рендер внутри блока, просто пропускаем контент на выход
          tag0
        } else {
          // Для наружной обёртки react-dnd требуется только нативный тег:
          val qdBlOpt = state.jdArgs
            .jdRuntime
            .data
            .qdBlockLess
            .get( state.tagId )

          <.div(
            // У нас тут - контейнер контента внеблоковый.
            jdCssStatic.contentOuterS,
            state.jdArgs.jdRuntime.jdCss.contentOuter,

            // Сборка измерителя размеров тега:
            Measure {
              // Собираем callback реакции на ресайз: она должна слать таймштамп из Pot'а, чтобы контроллер мог разобрать,
              // актуален ли callback. Может быть неактуален, т.к. замер происходит в рамках requestAnimationFrame(), которая слишком асинхронна.
              val __onResizeF = ReactCommonUtil.cbFun1ToJsCb(
                blocklessQdContentBoundsMeasuredJdCb(
                  for {
                    qdBl <- qdBlOpt
                    if qdBl.isPending
                    qdBlPend = qdBl.asInstanceOf[PendingBase]
                  } yield {
                    qdBlPend.startTime
                  }
                )
              )

              // Локальная переменная для блокирования бесконечного вызова mkChildrenF().
              // Вызов args.measure() вызывает тело mkChildrenF() из requestAnimationFrame().
              // Без ручной переменной isReMeasured снаружи от mkChildrenF() рендер будет бесконечен после однократного вызова measure().
              var isReMeasured = false

              def __mkChildrenF(chArgs: MeasureChildrenArgs): raw.PropsChildren = {
                // Запустить повторное измерение, когда это требуется состоянием.
                if (
                  !isReMeasured && qdBlOpt.exists(_.isPending)
                ) {
                  isReMeasured = true
                  chArgs.measure()
                }

                // ref должен быть прямо в теге как можно ближе к контенту, чтобы правильно измерить размер react-measure.
                tag0(
                  ^.genericRef := chArgs.measureRef,
                )
                  .rawElement
              }
              new MeasureProps {
                override val client = true
                override val bounds = true
                override val children = __mkChildrenF
                override val onResize = __onResizeF
              }
            }
          )
        }
      }

      /** Реакция на получение информации о размерах внеблокового qd-контента. */
      def blocklessQdContentBoundsMeasuredJdCb(timeStampMs: Option[Long])(cr: ContentRect): Callback

      def _qdBoundsMeasured(mp: ModelProxy[MJdRrrProps], timeStampMs: Option[Long], cr: ContentRect): QdBoundsMeasured = {
        val m = mp.value
        QdBoundsMeasured(m.subTree.rootLabel, m.tagId, timeStampMs, cr)
      }

    }

    def mkQdContent(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement


    // -------------------------------------------------------------------------------------------------

    trait BlockBase {

      /** Доп.наполнение для тега фоновой картинки блока. */
      def _bgImgAddons(bgImgData: MJdEdgeId, edge: MEdgeDataJs, state: MJdRrrProps): TagMod = {
        val jdt = state.subTree.rootLabel
        // За пределами редактора: только векторные картинки подлежат эмуляции кропа на клиенте (кроме wide-блоков):
        val isVector =
          bgImgData.outImgFormat.exists(_.isVector)

        OptionUtil.maybe(
          // В редакторе: все картинки - всегда оригиналы с эмуляцией кропа на клиенте:
          jdt.props1.expandMode.isEmpty ||
          state.jdArgs.conf.isEdit
        ) {
          // Размеры и позиционирование фоновой картинки в блоке (эмуляция кропа):
          val whOpt = jdt.props1.wh
          val origWh = edge.origWh
          val szMult = state.jdArgs.conf.szMult

          ImgUtilRJs.htmlImgCropEmuAttrsOpt(
            cropOpt     = bgImgData.crop,
            outerWhOpt  = whOpt,
            origWhOpt   = origWh,
            szMult      = szMult,
          )
            .orElse {
              // Кроп не задан. Отталкиваемся от высоты блока.
              for (wh <- whOpt) yield {
                val szMultedF = MSzMult.szMultedF( szMult )
                if (wh.width > wh.height)
                  ^.width := szMultedF(wh.width).px
                else
                  ^.height := szMultedF(wh.height).px
              }
            }
            .get
        }
          .getOrElse {
            // Просто заполнение всего блока картинкой. Т.к. фактический размер картинки отличается от размера блока
            // на px ratio, надо подогнать картинку по размерам:
            val css0 = state.jdArgs.jdRuntime.jdCss.blockBgF( state.tagId ): TagMod

            // Векторные фоновые картинки на wide-карточках желательно отпедалировать по вертикали.
            // Для wideVec нужно добавить вертикальную центровку картинки.
            (for {
              expandMode      <- jdt.props1.expandMode
              origWh          <- edge.origWh
              blockHeightPx   <- jdt.props1.heightPx
            } yield {
              val img2BlockRatio = state.jdArgs.conf.plainWideBlockWidthPx.toDouble / origWh.width.toDouble
              val wideSzMultOpt = OptionUtil.maybeOpt(expandMode.hasWideSzMult)(
                state.jdArgs.jdRuntime.data.jdtWideSzMults.get( state.tagId )
              )
              val blockHeightMultedPx = MSzMult.szMultedF( state.jdArgs.conf.szMult )(blockHeightPx, wideSzMultOpt)
              val offsetTopPx = (blockHeightMultedPx - (origWh.height * img2BlockRatio)) / 2
              TagMod(
                css0,
                ^.marginTop := offsetTopPx.px,
              )
            })
              .getOrElse( css0 )
          }
      }

      /** Доп.наполнение для sm-block div'a. */
      def _smBlockAddons(state: MJdRrrProps): TagMod =
        TagMod.empty

      /** Доп.наполнения для самого внешнего div'а. */
      def _outerContainerAddons(state: MJdRrrProps): TagMod =
        TagMod.empty


      def _renderBlockTag(propsProxy: ModelProxy[MJdRrrProps]): TagOf[html.Div] = {
        val state = propsProxy.value
        val s = state.subTree.rootLabel
        val C = state.jdArgs.jdRuntime.jdCss

        val isWide = s.props1.expandMode.nonEmpty

        val blockCss = jdCssStatic.smBlockS: TagMod

        // Обычное отображение, просто вернуть блок.
        val block = <.div(
          blockCss,
          C.smBlock,
          C.blockF( state.tagId ),

          // Статические и полустатические css-стили
          jdCssStatic.contentOuterS,
          C.contentOuter,

          // Наконец, рендер дочерних элементов jd-дерева.
          pureChildren( renderChildrenWithId( propsProxy ) )
            .toVdomArray,
        )

        val container = if (isWide) {
          // Широкоформатное отображение, рендерим фон без ограничений блока:
          <.div(
            C.wideContF(state.tagId),
            blockCss,
            block
          )
        } else {
          block
        }

        // Наборчик vdom-аттрибутов для внешнего контейнера текущего блока:
        container(

          // Скрывать не-main-стрипы, если этого требует рендер.
          // Это касается только стрипов, у которых нет isMain = Some(true)
          ReactCommonUtil.maybe(
            state.jdArgs.renderArgs.hideNonMainStrips &&
            !s.props1.isMain.getOrElseFalse
          ) {
            // Данный стип надо приглушить с помощью указанных css-стилей.
            ^.visibility.hidden
          },

          _bgColorOpt(s, state.jdArgs),

          // Для maybeSelected в редакторе:
          _outerContainerAddons(state),

          // Объединение цветом группы блоков для раскрытой карточки.
          state.jdArgs.renderArgs
            .groupOutLined
            .whenDefined { mcd =>
              TagMod(
                C.blockGroupOutline,
                ^.outlineColor := mcd.hexCode,
              )
            },

          // Если задана фоновая картинка, от отрендерить её.
          (for {
            bgImgData <- s.props1.bgImg
            edge      <- state.jdArgs.data.edges.get( bgImgData.edgeUid )
            if edge.jdEdge.predicate ==>> MPredicates.JdContent.Image
            bgImgSrc  <- edge.origImgSrcOpt
          } yield {
            <.img(
              ^.`class` := Css.Block.BG,
              ^.src := bgImgSrc,

              _bgImgAddons(bgImgData, edge, state),

              // В jdArgs может быть задан дополнительные модификации изображения, если selected tag.
              state.jdArgs.renderArgs.selJdtBgImgMod
                .filter(_ => state.isCurrentSelected)
                .whenDefined
            )
          })
            .whenDefined,

          _smBlockAddons(state),
        )
      }

    }

    def mkBlock(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement


    // -------------------------------------------------------------------------------------------------

    /** Базовый трейт для рендера компонента документа. */
    trait DocumentBase {

      def _renderGrid(propsProxy: ModelProxy[MJdRrrProps]): VdomElement = {
        val state = propsProxy.value

        // Плитку отсюда полностью вынести не удалось.
        CSSGrid {
          GridBuilderUtilJs.mkCssGridArgs(
            gbRes = state.gridBuildRes.getOrElse {
              LOG.error( ErrorMsgs.GRID_BUILD_RES_MISSING, msg = state.tagId.toString )
              throw new NoSuchElementException( ErrorMsgs.GRID_BUILD_RES_MISSING )
            },
            conf = state.jdArgs.conf,
            tagName = GridComponents.DIV,
          )
        } (
          renderChildrenWithId( propsProxy )
            // Тут тег-обёртка-костыль - что для CSSGrid нужен обязательно теги, а НЕ react-компоненты в children.
            .map { case (id, jdTree, p) =>
              <.div(
                ^.key := id.toString,
                fixZIndexIfBlock( jdTree.rootLabel ),
                p
              )
            }: _*
        )
      }

    }

    /** Сборка инстанса компонента. Здесь можно навешивать hoc'и и прочее счастье. */
    def mkDocument(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement


    // -------------------------------------------------------------------------------------------------

    /**
      * Запуск рендеринга произвольных тегов.
      */
    // TODO parent может быть необязательным. Но это сейчас не востребовано, поэтому он обязательный
    def renderTag( proxy: ModelProxy[MJdRrrProps] ): VdomElement = {
      import MJdTagNames._
      val p = proxy.value
      p.subTree.rootLabel.name match {
        case QD_CONTENT                => mkQdContent( p.tagId.toString, proxy )
        case STRIP                     => mkBlock( p.tagId.toString, proxy )
        case DOCUMENT                  => mkDocument( p.tagId.toString, proxy )
        // Это пока не вызывается, т.к. QD_OP отрабатывается в QdRrrHtml.
        case other =>
          throw new UnsupportedOperationException( other.toString )
      }
    }


    /** Отрендерить дочерние элементы тега обычным методом.
      * @return Итератор отрендеренных vdom-узлов.
      */
    def renderChildrenWithId(proxy: ModelProxy[MJdRrrProps]): Stream[(MJdTagId, Tree[JdTag], VdomNode)] = {
      val p = proxy.value
      val chs = p.subTree.subForest

      if (chs.nonEmpty) {
        val parent = p.subTree.rootLabel
        // Для qd-content-тегов надо указывать родительский тег.
        val parentSome = OptionUtil.maybe(parent.name ==* MJdTagNames.STRIP)( parent )
        chs
          .zipWithIndex
          .map { case (childJdTree, i) =>
            val ch = childJdTree.rootLabel
            val tagId = p.tagId.copy(
              selPathRev  = i :: p.tagId.selPathRev,
              blockExpand = if (ch.name ==* MJdTagNames.STRIP) {
                ch.props1.expandMode
              } else {
                p.tagId.blockExpand
              }
            )
            val p2 = p.copy(
              subTree = childJdTree,
              tagId   = tagId,
              parent  = parentSome,
            )
            // Вместо wrap используем прямой зум, чтобы избежать вызова цепочек функций, создающих множественные
            // инстансы одинаковых MJdRrrProps, которые будут удлиняться с каждым под-уровнем.
            val proxy2 = proxy.resetZoom( p2 )
            (tagId, childJdTree, renderTag(proxy2) )
          }
      } else {
        Stream.empty
      }
    }

    def pureChildren(chs: Stream[(MJdTagId, Tree[JdTag], VdomNode)]): Stream[VdomNode] = {
      chs.map(_._3)
    }


    val anyTagComp = ScalaComponent
      .builder[ModelProxy[MJdArgs]]( jdR.getClass.getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .render_P { propsProxy =>
        propsProxy.wrap { jdArgs =>
          MJdRrrProps(
            subTree = jdArgs.data.doc.template,
            tagId   = jdArgs.data.doc.jdId,
            jdArgs  = jdArgs,
          )
        }( renderTag )(implicitly, MJdRrrProps.MJdRrrPropsFastEq)
      }
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdArgs.MJdArgsFastEq) )
      .build

  }


  /** Обычная рендерилка. */
  object JdRrr extends JdRrrBase {

    /** Реализация контента. */
    final class QdContentB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) extends QdContentBase {

      /** Реакция на получение информации о размерах внеблокового qd-контента. */
      override def blocklessQdContentBoundsMeasuredJdCb(timeStampMs: Option[Long])(cr: ContentRect): Callback = {
        ReactDiodeUtil.dispatchOnProxyScopeCBf($) {
          propsProxy: ModelProxy[MJdRrrProps] =>
            _qdBoundsMeasured( propsProxy, timeStampMs, cr )
        }
      }

      /** Фасад для рендера. */
      def render(state: MJdRrrProps): VdomElement =
        _doRender(state)

    }
    /** Сборка react-компонента. def - т.к. в редакторе может не использоваться. */
    val qdContentComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[QdContentB].getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .renderBackend[QdContentB]
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdRrrPropsFastEq) )
      .build
    def mkQdContent(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      qdContentComp.withKey(key)(props)


    /** Реализация блока для обычного рендера. */
    final class BlockB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) extends BlockBase {

      def render(propsProxy: ModelProxy[MJdRrrProps]): VdomElement = {
        _renderBlockTag(propsProxy)
      }

    }
    val blockComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[BlockB].getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .renderBackend[BlockB]
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdRrrPropsFastEq) )
      .build
    def mkBlock(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      blockComp.withKey(key)(props)


    /** Реализация документа для обычного рендера. */
    final class DocumentB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) extends DocumentBase {

      def render(propsProxy: ModelProxy[MJdRrrProps]): VdomElement = {
        _renderGrid( propsProxy )
      }

    }
    val documentComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[BlockB].getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .renderBackend[DocumentB]
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdRrrPropsFastEq) )
      .build
    /** Сборка инстанса компонента. Здесь можно навешивать hoc'и и прочее счастье. */
    override def mkDocument(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      documentComp.withKey(key)(props)

  }


  /** Рендер опционального цвета фона. */
  private def _bgColorOpt(jdTag: JdTag, jdArgs: MJdArgs): TagMod = {
    jdTag.props1.bgColor.whenDefined { mcd =>
      jdArgs.jdRuntime.jdCss.bgColorF( mcd.hexCode )
    }
  }


  private def _apply(jdArgsProxy: Props) = JdRrr.anyTagComp( jdArgsProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}
