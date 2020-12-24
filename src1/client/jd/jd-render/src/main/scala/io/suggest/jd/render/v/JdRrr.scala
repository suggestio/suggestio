package io.suggest.jd.render.v

import com.github.dantrain.react.stonecutter._
import com.github.souporserious.react.measure.{ContentRect, Measure, MeasureChildrenArgs, MeasureProps}
import diode.react.ModelProxy
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.css.Css
import io.suggest.grid.GridBuilderUtilJs
import io.suggest.img.{ImgCommonUtil, ImgUtilRJs}
import io.suggest.jd.render.m._
import io.suggest.jd.tags._
import io.suggest.jd.{MJdEdgeId, MJdTagId}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.{MEdgeDataJs, MPredicates}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sjs.common.async.AsyncUtil._
import io.suggest.spa.DiodeUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.{TagOf, VdomElement}
import japgolly.univeq._
import org.scalajs.dom.html
import scalacss.ScalaCssReact._
import scalaz.{EphemeralStream, Tree}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.2020 8:35
  * Description: Обычный рендерер для Jd-контента.
  * Общий код между реализациями JdR и JdEditR.
  */
final class JdRrr(
                   jdCssStatic        : JdCssStatic,
                   qdRrrHtml          : QdRrrHtml,
                 )
  extends Log
{

  /** Чтобы qd-blockless с ротацией не наезжал слоем на блоки, надо их развести по этажам плитки.
    * Вызывается при сборке CSSGrid children.
    *
    * @param jdt jd-тег.
    * @return Пустой или непустой TagMod.
    */
  def fixZIndexIfBlock(jdt: JdTag): TagMod =
    ReactCommonUtil.maybe( jdt.name ==* MJdTagNames.STRIP )( jdCssStatic.smBlockOuter )


  /** Рендер опционального цвета фона. */
  private def _bgColorOpt(jdTag: JdTag, jdArgs: MJdArgs): TagMod = {
    jdTag.props1.bgColor.whenDefined { mcd =>
      jdArgs.jdRuntime.jdCss.bgColorF( mcd.hexCode )
    }
  }


  /** Для разделения edit и read-only режима используется этот трейт,
    * реализующий только базовый рендер с возможностью внутреннего расширения.
    */
  trait Base {

    /** Рендер линии-обводки для плитки. */
    def _groupOutlineRender(state: MJdRrrProps): TagMod = {
      state.jdArgs.renderArgs
        .groupOutLined
        .whenDefined { rootBlkColor =>
          // Если в текущем или parent jdTag содержатся иные настройки outline.color, то взять их.
          val mcdOpt = (state.subTree.rootLabel :: state.parents)
            .iterator
            .flatMap(_.props1.outline)
            .nextOption()
            .fold [Option[MColorData]] ( Some(rootBlkColor) ) ( _.color )

          TagMod(
            state.jdArgs.jdRuntime.jdCss.blockGroupOutline,
            ^.outlineColor := mcdOpt.fold( "rgba(0,0,0,0)" )(_.hexCode),
          )
        }
    }


    /** Рендерер поддерева, содержащего jd-теги Qd-ops.
      * Props на вход - тот же, что и QdContentBase.
      * Явное разделение сделано, чтобы можно было между qd-контейнером и контентом вставлять ещё прослойки для нужд редактора.
      */
    trait QdContentBase {

      def qdRrr: IQdRrr

      def render(propsProxy: ModelProxy[MJdRrrProps]): VdomElement = {
        qdRrrHtml
          .QdRrr( qdRrr, propsProxy )
          .render()
      }

    }


    /** Контейнер для qd-контента.
      * Живёт отдельно, т.к. в редакторе resize вставляется между двумя компонентами,
      * чтобы resize-ползунки были повёрнутыми/трансформированы вместе с контентом.
      */
    trait QdContainerBase[P, S] {

      def $: BackendScope[P, S]

      def _getModelProxy: CallbackTo[ModelProxy[MJdRrrProps]]

      def _renderQdContentTag(state: MJdRrrProps, acc0: List[TagMod]): TagOf[html.Div] = {
        import state.jdArgs.jdRuntime.jdCss

        val qdTag = state.subTree.rootLabel

        var contTagModsAcc: List[TagMod] = (
          jdCssStatic.gridItemS ::
          _bgColorOpt( qdTag, state.jdArgs ) ::
          // Рендер qd-контента в html.
          acc0
        )

        // Поддержка абсолютного позиционирования внутри контейнера:
        val absPosWideK = state
          .parents
          .iterator
          .flatMap( JdCss.wideWidthRatio(_, state.jdArgs.conf) )
          .nextOption()

        val absPosStyl = jdCss.absPosF( state.tagId -> absPosWideK ): TagMod

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
          if ( state.qdBlOpt.exists(_.nonEmpty) )
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
          contTagModsAcc ::= jdCss.lineHeightF( lineHeightPx -> JdCss.lineHeightJdIdOpt(state.tagId, qdTag) )

        <.div( contTagModsAcc : _* )
      }

      def _doRender(propsProxy: ModelProxy[MJdRrrProps], children: PropsChildren): TagOf[html.Div] = {
        // Если рендер ВНЕ блока, то нужно незаметно измерить высоту блока.
        val state = propsProxy.value
        val tag0 = _renderQdContentTag( state, children :: Nil )

        if (
          state.parents
            .exists(_.name ==* MJdTagNames.STRIP)
        ) {
          // Рендер внутри блока, просто пропускаем контент на выход
          tag0

        } else {
          // Для наружной обёртки react-dnd требуется только нативный тег:
          val jdCss = state.jdArgs.jdRuntime.jdCss

          val tag1 = tag0( jdCss.qdBlOuter )

          // Флаг для костыля принудительного ручного re-measure в mkChildrenF().
          // Отнесена по-дальше от самой функции, т.к. есть подозрение, что эта внешняя переменная склонна к
          // самозабвению из-за каких-то косяков в sjs-компиляторе.
          var isReMeasured = false

          <.div(
            // У нас тут - контейнер контента внеблоковый.
            jdCssStatic.contentOuterS,
            jdCss.contentOuter,
            jdCss.qdBlF( state.tagId ),

            // Если карточка имеет цвет outline-выделения, то выделить и контент.
            // При ротации никакой обводки не делаем, т.к. это имеет какой-то смысл делать только внутри контейнера с ротацией.
            // TODO Возможно, вообще никакой qd-bl-обводки не делать? Или перенести это внутрь _renderQdContentTag()?
            ReactCommonUtil.maybe(state.subTree.rootLabel.props1.rotateDeg.isEmpty)(
              _groupOutlineRender(state)
            ),

            // Сборка измерителя размеров тега:
            Measure {
              // Собираем callback реакции на ресайз: она должна слать таймштамп из Pot'а, чтобы контроллер мог разобрать,
              // актуален ли callback. Может быть неактуален, т.к. замер происходит в рамках requestAnimationFrame(), которая слишком асинхронна.
              val __onResizeF = ReactCommonUtil.cbFun1ToJsCb(
                blocklessQdContentBoundsMeasuredJdCb(
                  for {
                    qdBl      <- state.qdBlOpt
                    qdBlPend  <- qdBl.pendingOpt
                  } yield {
                    qdBlPend.startTime
                  }
                )
              )

              // Функция __mkChildrenF() вызывается 2-3 раза с одного react-рендера!
              def __mkChildrenF(chArgs: MeasureChildrenArgs): raw.PropsChildren = {
                // Костыль для принудительного запуска одного повторного измерения, если в состоянии задано (Pot[QdBl*].pending).
                // Запуск измерения вызывает повторный __mkChildrenF(), поэтому нужна максимально жесткая и гарантированная
                // система предотвращения бесконечного цикла.
                isReMeasured.synchronized {
                  if (!isReMeasured && state.qdBlOpt.exists(_.isPending)) {
                    isReMeasured = true
                    Future {
                      chArgs.measure()
                    }
                  }
                }

                // ref должен быть прямо в теге как можно ближе к контенту, чтобы правильно измерить размер react-measure.
                tag1(
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
            // TODO Почему-то не работает MeasureR здесь: сигналы в JdAh приходят, но рендер некорректен, и видимого результата не происходит. Надо исправить, чтобы MeasureR работал.
            /*
            measureR.component {
              measureR.PropsVal(
                onMeasured = blocklessQdContentBoundsMeasuredJdCb(
                  for {
                    qdBl      <- state.qdBlOpt
                    qdBlPend  <- qdBl.pendingOpt
                  } yield {
                    qdBlPend.startTime
                  }
                ),
                isToMeasure = () => state.qdBlOpt.exists(_.isPending),
                mBounds = true,
                mClient = true,
                childrenTag = tag1,
              )
            },
            */
          )
        }
      }

      /** Реакция на получение информации о размерах внеблокового qd-контента. */
      def blocklessQdContentBoundsMeasuredJdCb(timeStampMs: Option[Long])(cr: ContentRect): Callback = {
        _getModelProxy >>= { propsProxy =>
          val m = propsProxy.value
          val action = QdBoundsMeasured(m.subTree.rootLabel, m.tagId, timeStampMs, cr)
          propsProxy.dispatchCB( action )
        }
      }

    }

    def mkQdContainer(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement


    // -------------------------------------------------------------------------------------------------

    trait BlockBase {

      /** Доп.наполнение для тега фоновой картинки блока. */
      def _bgImgAddons(bgImgData: MJdEdgeId, edge: MEdgeDataJs, state: MJdRrrProps): TagMod = {
        val jdt = state.subTree.rootLabel
        import state.jdArgs.conf.{szMultF => szMultedF}

        // За пределами редактора: только векторные картинки подлежат эмуляции кропа на клиенте (кроме wide-блоков):
        OptionUtil.maybe(
          jdt.props1.expandMode.isEmpty ||
          // В редакторе: все фоновые картинки блоков- всегда оригиналы с эмуляцией кропа на клиенте:
          state.jdArgs.conf.isEdit
        ) {
          // Размеры и позиционирование фоновой картинки в блоке (эмуляция кропа):
          val whOpt = jdt.props1.wh
          val origWh = edge.origWh

          // При обычном рендере кропа обычно нет, поэтому сразу идёт перескок на orElse
          ImgUtilRJs.htmlImgCropEmuAttrsOpt(
            cropOpt     = bgImgData.crop,
            outerWhOpt  = whOpt,
            origWhOpt   = origWh,
            szMult      = state.jdArgs.conf.szMult,
          )
            .orElse {
              // Кроп не задан и это не редактор. Значит - это уже подготовленная сервером картинка под блок. Отталкиваемся от высоты блока:
              for (wh <- whOpt) yield {
                if (wh.width > wh.height)
                  ^.width := szMultedF(wh.width).px
                else
                  ^.height := szMultedF(wh.height).px
              }
            }
            .get

        }.getOrElse {
          // Широкая карточка за пределами редактора.
          var tmAcc = List.empty[TagMod]

          for {
            origWh <- edge.origWh
            blockHeightPx   <- jdt.props1.heightPx
          } {
            val wideSzMultOpt = for {
              expandMode <- jdt.props1.expandMode
              if expandMode.hasWideSzMult
              r <- state.jdArgs.jdRuntime.data.jdtWideSzMults.get( state.tagId )
            } yield r

            val isUseWidthCalc = ImgCommonUtil.isUseWidthForBlockBg(
              blockHeightPx = blockHeightPx,
              origWh        = origWh,
              jdt           = jdt,
              jdConf        = state.jdArgs.conf,
              wideSzMultOpt = wideSzMultOpt
            )

            tmAcc ::= {
              if (isUseWidthCalc.isUseWidth) {
                ^.width := isUseWidthCalc.contWidthPx.px
              } else {
                ^.height := isUseWidthCalc.blockHeightMultedPx.px
              }
            }

            // Векторные фоновые картинки на wide-карточках желательно отпедалировать по вертикали.
            // Для wideVec нужно добавить вертикальную центровку картинки.
            if (jdt.props1.expandMode.nonEmpty && isUseWidthCalc.isUseWidth) {
              val offsetTopPx = (isUseWidthCalc.blockHeightMultedPx - (origWh.height * isUseWidthCalc.img2BlockRatioW)) / 2
              tmAcc ::= {
                ^.marginTop := offsetTopPx.px
              }
            }

          }

          if (tmAcc.isEmpty) TagMod.empty
          else if (tmAcc.tail.isEmpty) tmAcc.head
          else TagMod( tmAcc: _* )
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

        val blockCss = jdCssStatic.gridItemS: TagMod

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

        // Наборчик vdom для наиболее внешнего контейнера текущего блока:
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
          _groupOutlineRender(state),

          // Если задана фоновая картинка, от отрендерить её.
          (for {
            edge      <- state.current_p1BgImgEdgeOpt
            if edge.jdEdge.predicate ==>> MPredicates.JdContent.Image
            bgImgSrc  <- edge.origImgSrcOpt
            bgImgData <- s.props1.bgImg
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
              logger.error( ErrorMsgs.GRID_BUILD_RES_MISSING, msg = state.tagId.toString )
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
        case QD_CONTENT                => mkQdContainer( p.tagId.toString, proxy )
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
    def renderChildrenWithId(proxy: ModelProxy[MJdRrrProps]): LazyList[(MJdTagId, Tree[JdTag], VdomNode)] = {
      val p = proxy.value
      val chs = EphemeralStream.toIterable( p.subTree.subForest )

      if (chs.nonEmpty) {
        // Пробрасывать данные gridBuild в дочерние теги? Да, только если текущий тег - это документ.
        // Не надо пробрасывать данные grid build внутрь блоков, чтобы не вызывать кучу пере-рендера в редакторе,
        // где gridBuild вызывается очень часто (из-за qd-blockless и других причин).
        val gridBuild2 = p.gridBuildRes
          .filter(_ => p.subTree.rootLabel.name ==* MJdTagNames.DOCUMENT)

        val parents2 = p.subTree.rootLabel :: p.parents
        chs
          .iterator
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
              parents = parents2,
              gridBuildRes = gridBuild2,
              renderArgs = getRenderArgs( tagId, p.jdArgs ),
            )
            // Вместо wrap используем прямой зум, чтобы избежать вызова цепочек функций, создающих множественные
            // инстансы одинаковых MJdRrrProps, которые будут удлиняться с каждым под-уровнем.
            val proxy2 = proxy.resetZoom( p2 )
            (tagId, childJdTree, renderTag(proxy2) )
          }
          .to( LazyList )

      } else {
        LazyList.empty
      }
    }


    def pureChildren(chs: LazyList[(MJdTagId, Tree[JdTag], VdomNode)]): LazyList[VdomNode] = {
      chs.map(_._3)
    }

    def getRenderArgs(jdtId: MJdTagId, jdArgs: MJdArgs): MJdRenderArgs
    /** Сборка render args под текущий jdt-тег.
      * Обычные jdArgs.renderArgs -- глобальны для всгео рендера, и в редакторе всё перерендеривается на каждый чих.
      *
      * @param jdArgs Данные рендера.
      * @return MJdRenderArgs, оптимально подходящий для текущего рендера.
      */
    def getRenderArgs(jdArgs: MJdArgs): MJdRenderArgs =
      getRenderArgs( jdArgs.data.doc.tagId, jdArgs )

    val anyTagComp = ScalaComponent
      .builder[ModelProxy[MJdArgs]]( getClass.getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .render_P { propsProxy =>
        propsProxy.wrap { jdArgs =>
          MJdRrrProps(
            subTree = jdArgs.data.doc.template,
            tagId   = jdArgs.data.doc.tagId,
            jdArgs  = jdArgs,
            // Проброс renderArgs, т.к. скорее всего они уже выставлены наверху.
            renderArgs = getRenderArgs( jdArgs ),
          )
        }( renderTag )(implicitly, MJdRrrProps.MJdRrrPropsFastEq)
      }
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdArgs.MJdArgsFastEq) )
      .build

  }

}
