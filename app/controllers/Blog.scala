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
        Ok(_blogRecordTpl(record, preview=false))
      case None =>
        NotFound
    }
  }

  /**
   * Функция уудаление записи в блоге.
   * @param rec_id
   * @return
   */

  /**
   * пока для тестов используется  maybeAuthenticated потом заменить на  isAdminAction (раскоментировать абзац ниже! а следующий удалить)
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
    mapping(
      "id" ->   nonEmptyText,
      "title" ->  nonEmptyText,
      "description" ->   nonEmptyText,
      "bg_image" -> text,
      "bg_color" -> text,
      "text"  -> text
    ) ((id, title, description, bg_image, bg_color, text) => MBlog(id, title, description, bg_image, bg_color, text))
    ({rec: MBlog => Some(rec.id, rec.title, rec.description, rec.bg_image, rec.bg_color, rec.text) })
  )


  /**
   * Сабмит формы добавления новой бложной записи в базу
   * @return
   */

  def newRecS =  maybeAuthenticated { implicit pw_opt => implicit request =>
    postFormM.bindFromRequest.fold(
    formWithErrors => NotAcceptable,
    { rec => rec.save
      Redirect(routes.Blog.readOne(rec.id))
    }
    )
  }

  /**
   * Сабмит формы измененной бложной записи в базу
   * @param rec_id
   * @return
   */


  def editRecS (rec_id: String) = maybeAuthenticated { implicit pw_opt => implicit request =>
    MBlog.getById(rec_id) match {
      case Some(record) =>
        postFormM.bindFromRequest.fold(
        formWithErrors => NotAcceptable(editFormTpl(record, formWithErrors)),
        { record => record.save
          Redirect(routes.Blog.readOne(rec_id))
        }
        )
      case None =>
        NotFound
    }
  }

  /**
   *  Рендерим форму редактирования записи блога
   * @param rec_id
   * @return
   */

  def editRec  (rec_id: String)=  maybeAuthenticated { implicit pw_opt => implicit request =>
    MBlog.getById(rec_id) match {
      case Some(record) =>
        val formEdit = postFormM.fill(record)
        Ok(editFormTpl(record, formEdit))
      case None =>
        NotFound
    }
  }


  /**
   * Рендерим чистой форму для создания новой записи в блоге.
   * @param
   * @return
   */

  def newRec  = maybeAuthenticated { implicit pw_opt => implicit request =>
    Ok(addRecordTpl(postFormM))
  }

}