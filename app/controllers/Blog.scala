package controllers

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 17.05.13
 * Time: 17:03
 * To change this template use File | Settings | File Templates.
 */


import models.MBlog
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import util.{AclT, ContextT}
import views.html.blog._
import play.api.data.validation.Constraints._




object Blog extends Controller with ContextT with AclT {

  /**
   * Функция запроса всех записей блога
   * @return
   */

  def recordList = maybeAuthenticated { implicit pw_opt => implicit request =>
    val blogs = MBlog.getAll
    Ok(listTpl(blogs))
  }

  /**
   *Функция запроса запис в блоге по id.
   * @param blog_id
   * @return
   */

  def readOne(blog_id: String) = maybeAuthenticated { implicit pw_opt => implicit request =>
    val recordOpt = MBlog.getById(blog_id)
    recordOpt match {
    case Some(record) =>
    Ok(_blogRecordTpl(record))
    case None =>
    NotFound
    }
  }

  /**
   * Функция уудаление записи в блоге.
   * @param rec_id
   * @return
   */

  // def deleteRecAdmin (rec_id: String) =  isAdminAction { implicit pw_opt => implicit request =>
  //  val deleteRec = MBlog.delete(rec_id)
  //  Ok(deleteRec.toString)
  //}


   def deleteRecAdmin (rec_id: String) =  maybeAuthenticated { implicit pw_opt => implicit request =>
   val deleteRec = MBlog.delete(rec_id)
   Ok(deleteRec.toString)
  }


  /**
   * Определение параметров формы для создания записи в блоге.
   */

  val postFormM = Form(
    tuple(
    "id" ->   nonEmptyText,
    "title" ->  nonEmptyText,
    "description" ->   nonEmptyText,
    "bg_image" -> text,
    "bg_color" -> text,
    "text"  -> text
   )
)


 // val dataId = Map {
 //   case (title,description) =>
  // }

  /**
   * Функция получения параметров из вышеопределенной формы
   * @return
   */

 def newRecS =  maybeAuthenticated { implicit pw_opt => implicit request =>
    postFormM.bindFromRequest.fold(
      formWithErrors => NotAcceptable,
       {case (id, title, description, bg_image, bg_color, text1) =>
     val new_blog_record = MBlog(id = id, title = title, description = description, bg_image = bg_image, bg_color = bg_color, text = text1).save
      Redirect(routes.Blog.readOne(new_blog_record.id))
    }
  )
}


//  val saveForm = Form(
//    mapping (
//      "id"  ->  number,
//      "title" ->   text,
//      "description" -> text,
//      "bg_image" -> text,
//      "bg_color" -> text,
//      "text" -> text
//    )
//  )

//  val dataForm = Map ( id -> 18, title ->
 // )

  /**
   * Функция получения чистой формы для создания новой записи в блоге.
   * @param
   * @return
   */
  def newRec  = maybeAuthenticated { implicit pw_opt => implicit request =>
     Ok(postFormTpl(postFormM, isEdit = false))
 }



}