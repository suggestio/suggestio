package net.xdsoft.jodit.types

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|

@js.native
trait IStatusBar extends js.Object

@js.native
trait Observer extends js.Object

@js.native
trait IViewWithToolbar extends js.Object

// ../core/selection/select/Select
@js.native
trait Select extends js.Object

@js.native
trait Modes extends js.Object

trait CustomCommand[T] extends js.Object
trait CommandOptions extends js.Object {
  val stopPropagation: Boolean
}

@js.native
trait IUploader extends js.Object

@js.native
trait IFileBrowser extends js.Object


trait IWorkPlace extends js.Object {
  val editor: dom.html.Div | dom.html.Body
  val element: dom.html.Element
  val container: dom.html.Div
  val workplace: dom.html.Div
  val statusbar: IStatusBar
  val iframe: js.UndefOr[dom.html.IFrame]
  val editorWindow: dom.Window
  val observer: Observer
  val options: IViewOptions
}


@js.native
trait IJodit extends IViewWithToolbar {

  val isJodit: Boolean = js.native

  val options: Observer = js.native
  val observer: Observer = js.native
  val editor: dom.html.Element = js.native
  val element: dom.html.Element = js.native

  def getNativeEditorValue(): String = js.native
  def getEditorValue(removeSelectionMarkers: Boolean = js.native): String = js.native
  def setEditorValue(value: String = js.native, notChangeStack: Boolean = js.native): Unit = js.native

  def getReadOnly(): Boolean = js.native
  def setReadOnly(enable: Boolean): Unit = js.native

  def places: js.Array[IWorkPlace] = js.native
  def currentPlace: IWorkPlace = js.native
  def addPlace(source: dom.html.Element | String, options: IViewOptions = js.native): Unit = js.native
  def setCurrentPlace(place: IWorkPlace): Unit = js.native

  def value: String = js.native
  def text: String = js.native

  def editorDocument: dom.html.Document = js.native
  def ed: dom.html.Document = js.native

  def editorWindow: dom.Window = js.native
  def ew: dom.html.Document = js.native

  def createInside: ICreate = js.native
  def selection: Select = js.native
  def s: Select = js.native

  def getRealMode(): Modes = js.native
  def getMode(): Modes = js.native
  def mode: Modes = js.native
  def isEditorMode(): Boolean = js.native
  def toggleMode(): Unit = js.native

  def editorIsActive: Boolean = js.native
  def execCommand(command: String, showUI: js.Any = js.native, value: js.Any = js.native): js.Any = js.native
  def registerCommand(
                      commandNameOriginal: String,
                      command: CustomCommand[IJodit],
                      options: CommandOptions = js.native,
                     ): IJodit = js.native

  def registerHotkeyToCommand(hotkeys: String | js.Array[String],
                              commandName: String,
                              shouldStop: Boolean = js.native,
                             ): Unit = js.native

  def workplace: dom.html.Div = js.native
  def statusbar: IStatusBar = js.native
  def uploader: IUploader = js.native
  def filebrowser: IFileBrowser = js.native
  def iframe: js.UndefOr[dom.html.IFrame] = js.native

}
