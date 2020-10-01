package io.suggest.lk.nodes.form.r.pop

import com.materialui.{MuiButton, MuiButtonProps, MuiColorTypes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiLinearProgress, MuiLinearProgressProps, MuiList, MuiListItem, MuiListItemText, MuiMenuItem, MuiMenuItemProps, MuiProgressVariants, MuiTextField, MuiTextFieldProps, MuiTypoGraphy, MuiTypoGraphyProps}
import diode.FastEq
import diode.data.Pot
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
import scalaz.Tree

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

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

  case class PropsVal(
                       create     : Option[MCreateNodeS],
                       tree       : Pot[Tree[String]],
                       nodesMap   : Map[String, MNodeState],
                     ) {
    def createParentPath = create.flatMap(_.parentPath)
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  case class State(
                    openedSomeC                 : ReactConnectProxy[Some[Boolean]],
                    nameOptC                    : ReactConnectProxy[Option[MTextFieldS]],
                    idOptC                      : ReactConnectProxy[Option[MTextFieldS]],
                    saveBtnDisabledSomeC        : ReactConnectProxy[Some[Boolean]],
                    isPendingSomeC              : ReactConnectProxy[Some[Boolean]],
                    exceptionOptC               : ReactConnectProxy[Option[Throwable]],
                    propsTreeParentPathC        : ReactConnectProxy[Props_t],
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

    /** Реакция на отмену или сокрытие диалога. */
    private val onCloseClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      dispatchOnProxyScopeCB( $, CreateNodeCloseClick )
    }


    def render(propsProxy: Props, s: State): VdomElement = {
      crCtxP.consume { crCtx =>
        val platCss = platformCssStatic()

        val diaChs = List[VdomElement](
          // Заголовок окна:
          platformComponents.diaTitle( Nil )(
            crCtx.messages( MsgCodes.`New.node` ),
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
                  val selectOptions = props.tree
                    .toOption
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
                          props.nodesMap
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
                        nodesToRender
                          .iterator
                          .map[VdomNode] { case (mns, nodePathRev) =>
                            val nodePathStr = nodePathRev
                              .reverse
                              .tail
                              .mkString( pathDelimStr )
                            MuiMenuItem.component.withKey( nodePathStr )(
                              new MuiMenuItemProps {
                                override val value = nodePathStr
                              }
                            )(
                              nodeHeaderR.component(
                                propsProxy.resetZoom(
                                  nodeHeaderR.PropsVal(
                                    render = MNodeStateRender( mns, nodePathRev ),
                                    isAdv = false,
                                    asList = false,
                                  )
                                )
                              )
                            )
                          }
                          .toList
                      }
                    }
                    .getOrElse {
                      // Нет узлов, ничего не найдено.
                      val emptyEl = MuiMenuItem(
                        new MuiMenuItemProps {
                          override val value = emptyValue
                        }
                      )(
                        <.em(
                          crCtx.messages( MsgCodes.`Nothing.found` )
                        )
                      ): VdomElement
                      emptyEl :: Nil
                    }

                  // Селект родительского узла:
                  val _parentNodeMsg = crCtx.messages( MsgCodes.`Parent.node` ).rawNode
                  val parentPathOpt = props.createParentPath
                  val _nodePathValueStr = parentPathOpt
                    .fold( emptyValue )( _.mkString( pathDelimStr ) )
                  MuiTextField(
                    new MuiTextFieldProps {
                      //override val `type` = HtmlConstants.Input.select
                      override val label = _parentNodeMsg
                      override val select = true
                      override val fullWidth = true
                      override val onChange = onParentChangeCbF
                      override val error = parentPathOpt.isEmpty
                      override val value = _nodePathValueStr
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
                override val onClick = onCloseClickCbF
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
              override val onClose = onCloseClickCbF
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
    .initialStateFromProps { propsProxy =>
      // Сборка коннекшенов до полей:
      val mtfFeq = OptFastEq.Wrapped(FastEq.AnyRefEq)
      def __mkMtfConn(lens: Lens[MCreateNodeS, MTextFieldS]) =
        propsProxy.connect(_.create.map(lens.get))( mtfFeq )

      // Сборка состояния:
      State(

        openedSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.create.isDefined )
        },

        nameOptC = __mkMtfConn( MCreateNodeS.name ),

        idOptC = __mkMtfConn( MCreateNodeS.id ),

        saveBtnDisabledSomeC = propsProxy.connect { propsOpt =>
          val saveDisabled = propsOpt.create.fold(true) { props =>
            !props.isValid ||
            props.saving.isPending
          }
          OptionUtil.SomeBool( saveDisabled )
        },

        isPendingSomeC = propsProxy.connect { propsOpt =>
          val isPending = propsOpt.create.exists(_.saving.isPending)
          OptionUtil.SomeBool( isPending )
        },

        exceptionOptC = propsProxy.connect( _.create.flatMap(_.saving.exceptionOption) )( OptFastEq.Wrapped(FastEq.AnyRefEq) ),

        propsTreeParentPathC = propsProxy.connect(identity)( FastEqUtil[Props_t] { (a, b) =>
          (a.createParentPath ===* b.createParentPath) &&
          (a.tree ===* b.tree)
        }),
      )
    }
    .renderBackend[Backend]
    .build

}
