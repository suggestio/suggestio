package io.suggest.lk.nodes.form.r.pop

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiColorTypes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiLinearProgress, MuiLinearProgressProps, MuiList, MuiListItem, MuiListItemText, MuiMenuItem, MuiMenuItemProps, MuiModalCloseReason, MuiProgressVariants, MuiSelectProps, MuiTextField, MuiTextFieldProps, MuiTypoGraphy, MuiTypoGraphyProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ble.BleConstants.Beacon.EddyStone
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.nodes.form.r.tree.NodeHeaderR
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.scalaz.NodePath_t
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.spa.{FastEqUtil, OptFastEq}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import monocle.Lens

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 21:31
  * Description: Компонент попапа с формой создания узла.
  */
class CreateNodeR(
                   platformCssStatic      : () => PlatformCssStatic,
                   platformComponents     : PlatformComponents,
                   nodeHeaderR            : NodeHeaderR,
                   crCtxP                 : React.Context[MCommonReactCtx],
                 ) {

  case class TreeParentPath(
                             tree: MTree,
                             parentPath: Option[NodePath_t],
                           )
  implicit val tppFeq = FastEqUtil[TreeParentPath] { (a, b) =>
    (a.tree ===* b.tree) &&
    (a.parentPath ===* b.parentPath)
  }

  type Props_t = MLkNodesRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    openedSomeC                 : ReactConnectProxy[Some[Boolean]],
                    nameOptC                    : ReactConnectProxy[Option[MTextFieldS]],
                    idOptC                      : ReactConnectProxy[Option[MTextFieldS]],
                    saveBtnDisabledSomeC        : ReactConnectProxy[Some[Boolean]],
                    isPendingSomeC              : ReactConnectProxy[Some[Boolean]],
                    exceptionOptC               : ReactConnectProxy[Option[Throwable]],
                    propsTreeParentPathC        : ReactConnectProxy[TreeParentPath],
                  )

  private def __mkTextField(
                             labelI18n: String,
                             placeHolderI18n: String,
                             conn: ReactConnectProxy[Option[MTextFieldS]],
                             onChanged: js.Function1[ReactEventFromInput, Unit],
                             helpText: js.UndefOr[raw.React.Node] = js.undefined,
                           ): VdomElement = {
    MuiListItem()(
      conn { mtfOptProxy =>
        val mtfOpt = mtfOptProxy.value
        val _value = mtfOpt.fold("")(_.value)
        val _isValid = mtfOpt.exists(_.isValid)
        // mtfOpt.isEmpty => состояние не инициализировано => поле без состояния неактивно (disabled=true).
        val _isEnabled = mtfOpt.exists(_.isEnabled)
        MuiTextField(
          new MuiTextFieldProps {
            override val fullWidth = true
            override val label = labelI18n
            override val value = _value
            override val error = !_isValid
            override val placeholder = placeHolderI18n
            override val onChange = onChanged
            override val helperText = helpText
            override val required = true
            override val disabled = !_isEnabled
          }
        )()
      }
    )
  }

  private def _PARENT_PATH_DELIM = '.'

  class Backend($: BackendScope[Props, State]) {

    /** Callback для ввода названия добавляемого под-узла. */
    private val _onNameChanged = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val name = e.target.value
      dispatchOnProxyScopeCB( $, CreateNodeNameChange(name = name) )
    }

    /** Callback редактирования id создаваемого узла. */
    private val _onIdChanged = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val id = e.target.value
      dispatchOnProxyScopeCB( $, CreateNodeIdChange(id = id) )
    }

    /** Callback выставления родителя. */
    private val onParentChangeCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val nodePath = e.target.value
        .split( _PARENT_PATH_DELIM )
        .iterator
        .map(_.toInt)
        .toList: NodePath_t
      dispatchOnProxyScopeCB( $, CreateNodeParentChange( nodePath ) )
    }

    /** Реакция на кнопку "Сохранить". */
    private val onSaveClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      dispatchOnProxyScopeCB( $, CreateNodeSaveClick )
    }

    private def _onClose =
      dispatchOnProxyScopeCB( $, CreateNodeCloseClick )

    private val onCloseBtnClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      _onClose
    }

    /** Реакция на отмену или сокрытие диалога. */
    private val onCloseDiaCbF = ReactCommonUtil.cbFun2ToJsCb { (_: ReactEvent, closeReason: MuiModalCloseReason) =>
      Callback.when( closeReason !=* MuiModalCloseReason.backdropClick )(
        _onClose
      )
    }


    def render(propsProxy: Props, s: State): VdomElement = {
      val isUseNativeSelect = platformComponents.useComplexNativeSelect()
      val _selectProps = new MuiSelectProps {
        override val native = isUseNativeSelect
        override val variant = MuiTextField.Variants.standard
      }

      // Готовим функция-рендерер для всех элементов селекта:
      val renderF = if (isUseNativeSelect) {
        (nodePathStr: String, mnsNodePathRev: Either[String, (MNodeState, NodePath_t)]) =>
          <.option(
            ^.value := nodePathStr,

            mnsNodePathRev.fold[TagMod](
              { title =>
                TagMod(
                  ^.disabled := true,
                  title
                )
              },
              {case (mns, nodePathRev) =>
                mns.infoPot
                  .toOption
                  .flatMap( _.name )
                  .orElse( mns.nodeId )
                  .whenDefined
              },
            ),
          ): VdomElement
      } else {
        (nodePathStr: String, mnsNodePathRev: Either[String, (MNodeState, NodePath_t)]) =>
          MuiMenuItem.component.withKey( nodePathStr )(
            new MuiMenuItemProps {
              override val value = nodePathStr
              override val disabled = mnsNodePathRev.isLeft
            }
          )(
            mnsNodePathRev.fold[VdomNode](
              {title =>
                <.em(
                  title,
                )
              },
              { case (mns, nodePathRev) =>
                nodeHeaderR.component(
                  propsProxy.resetZoom(
                    nodeHeaderR.PropsVal(
                      render = MNodeStateRender( mns, nodePathRev ),
                      isAdv  = false,
                      asList = false,
                    )
                  )
                )
              }
            ),
          ): VdomElement
      }

      crCtxP.consume { crCtx =>
        val platCss = platformCssStatic()

        val diaChs = List[VdomElement](
          // Заголовок окна:
          platformComponents.diaTitle( Nil )(
            platformComponents.diaTitleText(
              crCtx.messages( MsgCodes.`New.node` ),
            ),
          ),

          // Содержимое диалога:
          MuiDialogContent()(
            MuiList()(

              // Название узла (маячка).
              __mkTextField(
                crCtx.messages( MsgCodes.`Name` ),
                crCtx.messages( MsgCodes.`Beacon.name.example` ),
                s.nameOptC,
                _onNameChanged,
              ),

              // id узла/маячка.
              __mkTextField(
                crCtx.messages( MsgCodes.`Identifier` ),
                EddyStone.EXAMPLE_UID,
                s.idOptC,
                _onIdChanged,
                helpText = "EddyStone-UID",
              ),

              // Выбор родительского узла:
              MuiListItem()(
                // Варианты родительских узлов:
                s.propsTreeParentPathC { propsProxy =>
                  val emptyValue = ""
                  val pathDelimStr = _PARENT_PATH_DELIM.toString

                  val props = propsProxy.value
                  val selectOptions = props.tree.idsTreeOpt
                    .flatMap { tree =>
                      // Подобрать подходящие узлы:
                      val nodesToRender = tree
                        .zipWithIndex
                        // Заменить индексы на pathRev, чтобы у каждого узла был рядом его путь в дереве (обратный путь).
                        .deepMapFold( List.empty: NodePath_t ) { (acc, tree0) =>
                          val (mns, i) = tree0.rootLabel
                          val pathRev = (i :: acc)
                          pathRev -> (mns, pathRev)
                        }
                        .flatten
                        .flatMap { case (treeId, v) =>
                          props.tree.nodesMap
                            .get( treeId )
                            .map( _ -> v )
                            .toEphemeralStream
                        }
                        .filter { case (mns, _) =>
                          (mns.role ==* MTreeRoles.Normal) &&
                          mns.infoPot.exists { info =>
                            (info.isAdmin contains[Boolean] true) &&
                            info.ntype.exists(_.userCanCreateSubNodes)
                          }
                        }
                      // Вернуть None, если нет ни одного подходящего узла, чтобы передать рендер в getOrElse-ветвь.
                      Option.when( !nodesToRender.isEmpty ) {
                        val elems = nodesToRender
                          .iterator
                          .map[VdomNode] { case mnsNodePathRev @ (mns, nodePathRev) =>
                            val nodePathStr = nodePathRev
                              .reverse
                              .tail
                              .mkString( pathDelimStr )

                            renderF( nodePathStr, Right(mnsNodePathRev) )
                          }
                          .toList

                        if (isUseNativeSelect) {
                          val id = MsgCodes.`Choose...`
                          val title = crCtx.messages( id )
                          val firstEmptyEl = renderF( id, Left(title) )
                          firstEmptyEl :: elems
                        } else {
                          elems
                        }
                      }
                    }
                    .getOrElse {
                      // Нет узлов, ничего не найдено.
                      val title = crCtx.messages( MsgCodes.`Nothing.found` )
                      val emptyEl = renderF(emptyValue, Left(title) )
                      emptyEl :: Nil
                    }

                  // Селект родительского узла:
                  val _parentNodeMsg = crCtx.messages( MsgCodes.`Parent.node` ).rawNode
                  val parentPathOpt = props.parentPath
                  val _nodePathValueStr = parentPathOpt
                    .fold {
                      if (isUseNativeSelect)
                        MsgCodes.`Choose...`
                      else
                        emptyValue
                    }( _.mkString( pathDelimStr ) )

                  MuiTextField(
                    new MuiTextFieldProps {
                      override val label = _parentNodeMsg
                      override val select = true
                      override val fullWidth = true
                      override val onChange = onParentChangeCbF
                      override val error = parentPathOpt.isEmpty
                      override val value = _nodePathValueStr
                      override val SelectProps = _selectProps
                      override val variant = MuiTextField.Variants.standard
                    }
                  )(
                    selectOptions: _*
                  )
                }
              ),

              // pending progress bar
              s.isPendingSomeC { isPendingSomeProxy =>
                val isPending = isPendingSomeProxy.value.value
                <.span(
                  if (isPending) ^.visibility.visible else ^.visibility.hidden,
                  MuiLinearProgress(
                    new MuiLinearProgressProps {
                      override val variant = if (isPending) MuiProgressVariants.indeterminate else MuiProgressVariants.determinate
                      override val value = JsOptionUtil.maybeDefined( !isPending )(0)
                    }
                  )
                )
              },

              s.exceptionOptC { exceptionOptProxy =>
                exceptionOptProxy.value.whenDefinedEl { ex =>
                  MuiListItem()(
                    MuiListItemText()(
                      MuiTypoGraphy(
                        new MuiTypoGraphyProps {
                          override val color = MuiColorTypes.error
                        }
                      )(
                        ex match {
                          case ex: LknException =>
                            <.span(
                              crCtx.messages( ex.msgCode ),
                              HtmlConstants.SPACE,
                              ex.titleOpt.whenDefined( crCtx.messages(_) ),

                              HtmlConstants.SPACE, HtmlConstants.`(`,
                              ex.getCause.getClass.getSimpleName,
                              HtmlConstants.`)`,
                            )
                          case other => other.getMessage
                        }
                      )
                    )
                  )
                }
              },
            ),

          ),

          // Кнопки внизу окна:
          MuiDialogActions(
            platformComponents.diaActionsProps()(platCss)
          )(
            // Кнопка "Сохранить"
            {
              val saveMsg = crCtx.messages( MsgCodes.`Save` )
              s.saveBtnDisabledSomeC { saveBtnDisabledSomeProxy =>
                MuiButton(
                  new MuiButtonProps {
                    override val onClick = onSaveClickCbF
                    override val size = MuiButtonSizes.large
                    override val disabled = saveBtnDisabledSomeProxy.value.value
                  }
                )(
                  saveMsg,
                )
              }
            },

            // Кнопка "Закрыть"
            MuiButton(
              new MuiButtonProps {
                override val onClick = onCloseBtnClickCbF
                override val size = MuiButtonSizes.large
              }
            )(
              crCtx.messages( MsgCodes.`Close` ),
            ),
          ),

        )

        // Наконец, рендер окна самого диалога:
        val diaCss = new MuiDialogClasses {
          override val paper = platCss.Dialogs.paper.htmlClass
        }
        s.openedSomeC { openedSomeProxy =>
          MuiDialog(
            new MuiDialogProps {
              override val open = openedSomeProxy.value.value
              @JSName("onClose")
              override val onClose2 = onCloseDiaCbF
              override val maxWidth = MuiDialogMaxWidths.sm
              override val fullWidth = true
              override val classes = diaCss
            }
          )( diaChs: _* )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { rootProxy =>
      val propsOptProxy = rootProxy.zoom( _.popups.createNodeS )

      // Сборка коннекшенов до полей:
      val mtfFeq = OptFastEq.Wrapped(FastEq.AnyRefEq)
      def __mkMtfConn(lens: Lens[MCreateNodeS, MTextFieldS]) =
        propsOptProxy.connect(_.map(lens.get))( mtfFeq )

      // Сборка состояния:
      State(

        openedSomeC = propsOptProxy.connect { propsOpt =>
          OptionUtil.SomeBool( propsOpt.isDefined )
        },

        nameOptC = __mkMtfConn( MCreateNodeS.name ),

        idOptC = __mkMtfConn( MCreateNodeS.id ),

        saveBtnDisabledSomeC = propsOptProxy.connect { propsOpt =>
          val saveDisabled = propsOpt.fold(true) { props =>
            !props.isValid ||
            props.saving.isPending
          }
          OptionUtil.SomeBool( saveDisabled )
        },

        isPendingSomeC = propsOptProxy.connect { propsOpt =>
          val isPending = propsOpt.exists(_.saving.isPending)
          OptionUtil.SomeBool( isPending )
        },

        exceptionOptC = propsOptProxy.connect( _.flatMap(_.saving.exceptionOption) )( OptFastEq.Wrapped(FastEq.AnyRefEq) ),

        propsTreeParentPathC = rootProxy.connect { mroot =>
          TreeParentPath(
            tree = mroot.tree.tree,
            parentPath = mroot.popups.createNodeS.flatMap(_.parentPath),
          )
        }(tppFeq),
      )
    }
    .renderBackend[Backend]
    .build

}
