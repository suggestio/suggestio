package io.suggest.n2.edge.edit.v.inputs.media

import com.materialui.{Mui, MuiCheckBox, MuiCheckBoxProps, MuiCircularProgress, MuiCircularProgressProps, MuiColorTypes, MuiFormControlClasses, MuiFormControlLabel, MuiFormControlLabelClasses, MuiFormControlLabelProps, MuiInput, MuiInputClasses, MuiInputProps, MuiLink, MuiLinkProps, MuiList, MuiListItem, MuiListItemIcon, MuiListItemText, MuiMenuItem, MuiMenuItemProps, MuiPaper, MuiProgressVariants, MuiTable, MuiTableBody, MuiTableCell, MuiTableRow, MuiTableRowProps, MuiTextField, MuiTextFieldProps, MuiToolTip, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.n2.media.{MEdgeMedia, MFileMeta, MFileMetaHash, MFileMetaHashFlag, MFileMetaHashFlags}
import diode.react.ReactPot._
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.crypto.hash.{MHash, MHashes}
import io.suggest.form.MFormResourceKey
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.m.UploadFile
import io.suggest.n2.edge.edit.MNodeEdgeIdQs
import io.suggest.n2.edge.edit.m.{EdgeUpdateWithTraverse, FileHashEdit, FileHashFlagSet}
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.n2.media.storage.{MStorage, MStorageInfo, MStorageInfoData, MStorages}
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import io.suggest.n2.edge.MEdge
import io.suggest.routes.routes
import io.suggest.sjs.dom2.DomListSeq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.up.{MFileUploadS, MUploadResp}
import io.suggest.xplay.json.PlayJsonSjsUtil
import monocle.Traversal
import play.api.libs.json.Json

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.2020 22:18
  * Description: ??????????????????, ?????????????????????? media-???????????? ?? ????????.
  */
class MediaR(
              crCtxProv: React.Context[MCommonReactCtx],
            ) {

  case class PropsVal(
                       media        : Option[MEdgeMedia],
                       uploadReq    : MFileUploadS,
                       edgeIdQs     : Option[MNodeEdgeIdQs],
                     )
  implicit case object MediaRPropsValFeq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.media ===* b.media) &&
      (a.uploadReq ===* b.uploadReq) &&
      (a.edgeIdQs ===* b.edgeIdQs)
    }
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  case class State(
                    uploadReqPotC           : ReactConnectProxy[Pot[MUploadResp]],
                    uploadProgressC         : ReactConnectProxy[Option[Int]],
                    edgeMediaDefinedSomeC   : ReactConnectProxy[Some[Boolean]],
                    fileMimeOptC            : ReactConnectProxy[Option[String]],
                    fileSizeOptC            : ReactConnectProxy[Option[Long]],
                    fileIsOriginalSomeC     : ReactConnectProxy[Some[Boolean]],
                    fileHashesC             : ReactConnectProxy[Seq[MFileMetaHash]],
                    fileStorageTypeC        : ReactConnectProxy[MStorage],
                    fileStorageDataMetaC    : ReactConnectProxy[String],
                    edgeIdQsOptC            : ReactConnectProxy[Option[MNodeEdgeIdQs]],
                  )

  private def edge_media_LENS = MEdge.media
    .andThen( Traversal.fromTraverse[Option, MEdgeMedia] )
  private def edge_media_file_LENS =
    edge_media_LENS andThen MEdgeMedia.file
  private def root_edge_media_storage_LENS = {
    edge_media_LENS
      .andThen( MEdgeMedia.storage )
      .andThen( Traversal.fromTraverse[Option, MStorageInfo] )
  }

  class Backend($: BackendScope[Props, State]) {

    /** ?????????????? ???? ?????????????????? ?????????????????? ??????????. */
    private lazy val _onFileChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val files = DomListSeq( e.target.files )
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, UploadFile( files, MFormResourceKey.empty ) )
    }

    /** ???????????????????????????? MIME. */
    private lazy val _onFileMimeChanged = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val mimeOpt = (for {
        v <- Option( e.target.value )
        v2 = v.trim.toLowerCase()
        if v2.nonEmpty
      } yield {
        v2
      })
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EdgeUpdateWithTraverse(edge_media_file_LENS andThen MFileMeta.mime, mimeOpt) )
    }

    private lazy val _onFileSizeChanged = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val szBytes = Try( e.target.value.toLong )
        .toOption
        .filter(_ >= 0)
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EdgeUpdateWithTraverse(edge_media_file_LENS andThen MFileMeta.sizeB, szBytes) )
    }

    private lazy val _onFileIsOriginalChanged = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val isOriginal = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EdgeUpdateWithTraverse(edge_media_file_LENS andThen MFileMeta.isOriginal, isOriginal) )
    }

    private def _onFileHashChange(mhash: MHash) = {
      ReactCommonUtil.cbFun1ToJsCb { (e: ReactEventFromInput) =>
        Option( e.target.value )
          .map(_.trim.toLowerCase())
          .filter { hash =>
            hash.matches("[a-f0-9]*")
          }
          .fold( Callback.empty ) { hash2 =>
            ReactDiodeUtil.dispatchOnProxyScopeCB( $, FileHashEdit(mhash, hash2) )
          }
      }
    }

    private def _onFileHashFlagChange(mhash: MHash, flag: MFileMetaHashFlag) = {
      ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
        val isChecked = e.target.checked
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, FileHashFlagSet(mhash, flag, isChecked) )
      }
    }

    private lazy val _onFileStorageTypeChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val storage2 = MStorages.withValue( e.target.value )
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EdgeUpdateWithTraverse(root_edge_media_storage_LENS andThen MStorageInfo.storage, storage2) )
    }

    private lazy val _onFileStorageMetaDataChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val storageMeta2 = e.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EdgeUpdateWithTraverse(root_edge_media_storage_LENS andThen MStorageInfo.data andThen MStorageInfoData.meta, storageMeta2) )
    }

    def render(s: State): VdomElement = {

      val _inputCss = EdgeEditCss.input.htmlClass
      val _tfCss = new MuiFormControlClasses {
        override val root = _inputCss
      }

      // ???????????? ??????.
      lazy val _mediaEditors = crCtxProv.consume { crCtx =>
        React.Fragment(
          // MIME-?????? ??????????.
          MuiListItem()(
            {
              val _label = crCtx.messages( MsgCodes.`Mime.type` ): VdomNode
              s.fileMimeOptC { fileMimeOptProxy =>
                val _fileMime = fileMimeOptProxy.value getOrElse ""
                MuiTextField(
                  new MuiTextFieldProps {
                    override val value = _fileMime
                    override val `type` = HtmlConstants.Input.text
                    override val onChange = _onFileMimeChanged
                    override val label = _label.rawNode
                    override val classes = _tfCss
                    override val required = true
                    override val variant = MuiTextField.Variants.standard
                  }
                )()
              }
            },

            // ???????????? ?????????????? ??????????.
            {
              val _label = crCtx.messages( MsgCodes.`Size` ): VdomNode
              s.fileSizeOptC { fileSizeOptProxy =>
                val _fileSize = fileSizeOptProxy
                  .value
                  .fold("")(_.toString)
                MuiTextField(
                  new MuiTextFieldProps {
                    override val value = _fileSize
                    override val `type` = HtmlConstants.Input.number
                    override val onChange = _onFileSizeChanged
                    override val label = _label.rawNode
                    override val classes = _tfCss
                    override val required = true
                    override val variant = MuiTextField.Variants.standard
                  }
                )()
              }
            },

            // ???????? ?????????????????? ??????????
            MuiFormControlLabel {
              val _checkBox = s.fileIsOriginalSomeC { fileIsOriginalSomeProxy =>
                val isOrig = fileIsOriginalSomeProxy.value.value
                MuiToolTip {
                  new MuiToolTipProps {
                    override val title = crCtx.messages( MsgCodes.yesNo(isOrig) )
                  }
                } (
                  MuiCheckBox(
                    new MuiCheckBoxProps {
                      override val checked = isOrig
                      override val onChange = _onFileIsOriginalChanged
                      override val color = MuiColorTypes.secondary
                    }
                  )
                )
              }
              val _label = crCtx.messages( MsgCodes.`Original` )
              val _css = new MuiFormControlLabelClasses {
                override val root = EdgeEditCss.inputLeft.htmlClass
              }

              new MuiFormControlLabelProps {
                override val control = _checkBox.rawElement
                override val label = _label.rawNode
                override val classes = _css
              }
            },
          ),

          // ???????????? ?????????? ?? ???????????????????? ????????????????????????????.:
          MuiListItem()(
            s.fileHashesC { fileHashesProxy =>
              val fmHashes = fileHashesProxy
                .value
                .iterator
                .map { fmHash =>
                  fmHash.hType -> fmHash
                }
                .toMap

              MuiTable()(
                MuiTableBody()(
                  MHashes
                    .values
                    .toVdomArray { mhash =>
                      val fmHash = fmHashes.get( mhash )
                      MuiTableRow.component
                        .withKey( mhash.value )( new MuiTableRowProps {} )(
                          // ?????? ???????? (????????????????????: ?? ???????????? ?????????? ?????????????????????????? ?????? ????????).
                          MuiTableCell()(
                            mhash.fullStdName,
                          ),
                          // ???????????????? ????????:
                          MuiTableCell()(
                            MuiTextField {
                              val _value = fmHash
                                .fold("")( _.hexValue )
                              new MuiTextFieldProps {
                                override val label = mhash.fullStdName.rawNode
                                override val value = _value
                                override val onChange = _onFileHashChange( mhash )
                                override val required  = true
                                override val variant = MuiTextField.Variants.standard
                              }
                            }()
                          ),
                          // ?????????? ????????:
                          MuiTableCell()(
                            MFileMetaHashFlags
                              .values
                              .toVdomArray { fmFlag =>
                                val _hasFlag = fmHash
                                  .fold(false)(_.flags contains fmFlag)
                                val _cb = MuiCheckBox {
                                  new MuiCheckBoxProps {
                                    override val checked = _hasFlag
                                    override val onChange = _onFileHashFlagChange(mhash, fmFlag)
                                  }
                                }
                                val _cbLabel = fmFlag.toString
                                MuiFormControlLabel.component.withKey(fmFlag.value)(
                                  new MuiFormControlLabelProps {
                                    override val control  = _cb.rawElement
                                    override val label    = _cbLabel.rawNode
                                  }
                                )
                              }
                          )
                        )
                    }
                ),
              )
            },
          ),

          // edge.media.storage
          MuiListItem()(
            MuiListItemIcon()(
              Mui.SvgIcons.Storage()(),
            ),
            MuiListItemText()(
              crCtx.messages( MsgCodes.`Storage` )
            ),
          ),

          MuiListItem()(
            // ?????? ??????????????????
            {
              val storageOptions = MStorages.values
                .iterator
                .map[VdomNode] { storage =>
                  MuiMenuItem(
                    new MuiMenuItemProps {
                      override val value = storage.value
                    }
                  )(
                    MuiListItemText()(
                      storage.toString,
                    )
                  )
                }
                .toList
              val storageSelectorLabel = crCtx.messages(MsgCodes.`Type`): VdomNode
              s.fileStorageTypeC { fileStorageTypeProxy =>
                MuiTextField(
                  new MuiTextFieldProps {
                    override val select   = true
                    override val label    = storageSelectorLabel.rawNode
                    override val value    = fileStorageTypeProxy.value.value
                    override val onChange = _onFileStorageTypeChange
                    override val classes  = _tfCss
                    override val variant  = MuiTextField.Variants.standard
                  }
                )(storageOptions: _*)
              }
            },

            // ???????????? ???????????????????? ?? ?????????????????? ??????????????????.
            {
              val _label = crCtx.messages( MsgCodes.`Metadata` )
              s.fileStorageDataMetaC { fileStorageMetaDataProxy =>
                val fileStorageMetaData = fileStorageMetaDataProxy.value
                MuiTextField(
                  new MuiTextFieldProps {
                    override val `type`   = HtmlConstants.Input.text
                    override val label    = _label
                    override val value    = fileStorageMetaData
                    override val onChange = _onFileStorageMetaDataChange
                    override val classes  = _tfCss
                    override val required = true
                    override val error    = fileStorageMetaData.isEmpty
                    override val variant  = MuiTextField.Variants.standard
                  }
                )()
              }
            },

          ),
        )
      }

      lazy val _muiInputCss = new MuiInputClasses {
        override val root = _inputCss
      }

      lazy val _progress = s.uploadProgressC { uploadProgressProxy =>
        val progressOpt = uploadProgressProxy.value

        // TODO CircularProgressWithLabel ???????????? CircularProgress
        MuiCircularProgress(
          new MuiCircularProgressProps {
            override val variant = progressOpt.fold( MuiProgressVariants.indeterminate )( _ => MuiProgressVariants.determinate )
            override val value = progressOpt.toUndef
          }
        )
      }
      // ?????? ?????????????? ??????????. ?????? ???????????????????? ???? ???????????????????????? ??????????.
      lazy val _fileUpload = s.uploadReqPotC { uploadReqPotProxy =>
        val pot = uploadReqPotProxy.value

        React.Fragment(

          // ???????? ???????????? ?????? ???? ?????????????????? - ?????????????????????? input ?????? ??????????-??????????????.
          MuiListItem()(
            pot.renderEmpty {
              MuiInput(
                new MuiInputProps {
                  override val `type`   = HtmlConstants.Input.file
                  override val onChange = _onFileChange
                  override val error    = pot.isFailed
                  override val disabled = pot.isPending
                  override val classes  = _muiInputCss
                }
              )
            },

            pot.renderPending { _ =>
              _progress
            },
          ),

          // ???????????? ?????????????? ??????????.
          pot.renderFailed { ex =>
            MuiListItem()(
              MuiToolTip {
                val _title = <.pre(
                  ex.getStackTrace
                    .iterator
                    .take(6)
                    .mkString("\n"),
                )
                  .rawNode
                new MuiToolTipProps {
                  override val title = _title
                }
              } (
                MuiTypoGraphy(
                  new MuiTypoGraphyProps {
                    override val variant = MuiTypoGraphyVariants.h5
                  }
                )(
                  crCtxProv.message( MsgCodes.`Error` ),
                  HtmlConstants.SPACE,
                  ex.getClass.getSimpleName,
                  HtmlConstants.SPACE,
                  ex.getMessage,
                )
              ),
            )
          },

        )
      }

      // ?????????????? ???????????????? EdgeMedia ???? ??????????: ?????????? ?????? ?????????????????? ??????????????, ?? ?????????????????????????? ???????????? ?? ???????????????????????? ??????????.
      // ??????????, ???????? ???? ?????????????? ???????? ?????????? ????????????.

      MuiPaper()(
        MuiList()(

          // edge.media.file
          MuiListItem()(
            MuiListItemIcon()(
              Mui.SvgIcons.FileCopy()(),
            ),
            MuiListItemText()(
              crCtxProv.message( MsgCodes.`File` )
            ),

            // ???????? ???????? ????????, ???? ?????????????????????? ???????????? ?????? ???????????????? ??????????.
            {
              lazy val openFileMsg = crCtxProv.message( MsgCodes.`Open.file` )
              s.edgeIdQsOptC { edgeIdQsOptProxy =>
                edgeIdQsOptProxy.value.whenDefinedEl { edgeIdQs =>
                  val edgeIdQsJson = PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject(edgeIdQs) )
                  MuiLink(
                    new MuiLinkProps {
                      val href = routes.controllers.SysNodeEdges.openFile( edgeIdQsJson ).url
                    }
                  )(
                    openFileMsg,
                  )
                }
              }
            }
          ),

          s.edgeMediaDefinedSomeC { edgeMediaDefinedSomeProxy =>
            if (edgeMediaDefinedSomeProxy.value.value) {
              _mediaEditors
            } else {
              _fileUpload
            }
          },

        ),
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      val emptyStr = ""
      State(
        uploadReqPotC = propsProxy.connect( _.uploadReq.currentReq ),
        uploadProgressC = propsProxy.connect { props =>
          props.uploadReq.progress
            .flatMap(_.progressPercent)
            .filter { p => (p > 0) && (p < 100) }
        },
        edgeMediaDefinedSomeC = propsProxy.connect { m =>
          OptionUtil.SomeBool( m.media.nonEmpty )
        },
        fileMimeOptC = propsProxy.connect { props =>
          props.media
            .flatMap( _.file.mime )
        },
        fileSizeOptC = propsProxy.connect { props =>
          props.media
            .flatMap( _.file.sizeB )
        }( FastEq.ValueEq ),
        fileIsOriginalSomeC = propsProxy.connect { props =>
          val isOrig = props.media
            .fold(true)(_.file.isOriginal)
          OptionUtil.SomeBool( isOrig )
        },
        fileHashesC = propsProxy.connect { props =>
          props.media
            .fold[Seq[MFileMetaHash]]( Nil )( _.file.hashesHex )
        },
        fileStorageTypeC = propsProxy.connect { props =>
          props.media
            .flatMap( _.storage )
            .fold( MStorages.values.head )( _.storage )
        },
        fileStorageDataMetaC = propsProxy.connect { props =>
          props.media
            .flatMap(_.storage)
            .fold(emptyStr)( _.data.meta )
        },
        edgeIdQsOptC = propsProxy.connect { props =>
          props.edgeIdQs
            .filter(_ => props.media.nonEmpty)
        },
      )
    }
    .renderBackend[Backend]
    .build

}
