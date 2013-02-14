package controllers;

import business.GameController;
import com.linkedin.domination.api.Player;
import models.User;
import play.api.libs.MimeTypes;
import play.api.libs.iteratee.Enumerator;
import play.api.mvc.ResponseHeader;
import play.api.mvc.SimpleResult;
import play.data.Form;
import play.data.validation.Constraints.Required;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.Security;
import scala.collection.immutable.Map;
import scalax.io.support.FileUtils;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Application extends Controller {

    static Form<User> userForm = form(User.class);

    private static final String IMAGE_FILE_LOCATION = "storage/images";
    private static final String JAR_FILE_LOCATION = "storage/jars";
  
    public static Result index() {
        return ok("Hello world.");
    }

    public static Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return badRequest(views.html.login.render(loginForm));
        }
        else {
            System.out.println("LoginForm = " + loginForm);
            session().clear();
            session("username", loginForm.get().username);
            return redirect(routes.Application.userMain());
        }
    }

    public static Result login() {
        return ok(
                views.html.login.render(form(Login.class))
        );
    }

    public static Result adminAuthenticate() {
        Form<AdminLogin> loginForm = form(AdminLogin.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return badRequest(views.html.adminLogin.render(loginForm));
        }
        else {
            session().clear();
            session("adminUsername", loginForm.get().username);
            return redirect(routes.Application.users());
        }
    }

    public static Result adminLogin() {
        return ok(
                views.html.adminLogin.render(form(AdminLogin.class))
        );
    }

    public static Result logout() {
        session().clear();
        return ok(
                views.html.login.render(form(Login.class))
        );
    }

    public static Result signup() {
        return ok(
            views.html.signup.render(userForm)
        );
    }

    public static Result newUser() {
        Form<User> filledForm = userForm.bindFromRequest();
        if (filledForm.hasErrors()) {
            return badRequest(
                    views.html.signup.render(filledForm)
            );
        }
        else {
            User.create(filledForm.get());
            session().clear();
            System.out.println("Setting the session username of " + filledForm.get().name);
            session("username", filledForm.get().name);
            return redirect(routes.Application.userMain());
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result userMain() {
        String username = session().get("username");
        User user = User.getUserByName(username);
        System.out.println("Got a user object of " + user + " when using the name " + username);
        return ok(
                views.html.userMain.render(user)
        );
    }

    @Security.Authenticated(Secured.class)
    public static Result users() {
        return ok(
            views.html.users.render(User.all(), userForm)
        );
    }

    @Security.Authenticated(Secured.class)
    public static Result uploadImagePage() {
        String username = session().get("username");
        User user = User.getUserByName(username);
        return ok(views.html.uploadImage.render(user));
    }

    @Security.Authenticated(Secured.class)
    public static Result uploadImage() {
        String username = session().get("username");
        MultipartFormData body = request().body().asMultipartFormData();
        FilePart picture = body.getFile("picture");
        if (picture != null) {
            String fileName = picture.getFilename();
            String contentType = picture.getContentType();
            File file = picture.getFile();
            try {
                FileInputStream fis = new FileInputStream(file);
                File savedFile = new File(IMAGE_FILE_LOCATION + file.separator + username + ".jpg");
                FileOutputStream fos = new FileOutputStream(savedFile);
                FileUtils.copy(fis, fos);
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }

            return ok("File uploaded");
        }
        else {
            flash("error", "Missing file");
            return redirect(routes.Application.userMain());
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result image(String name) {
        File image = new File(IMAGE_FILE_LOCATION, name + ".jpg");
        return ok(image);
    }

    @Security.Authenticated(Secured.class)
    public static Result uploadJarPage() {
        String username = session().get("username");
        User user = User.getUserByName(username);
        return ok(views.html.uploadJar.render(user));
    }

    @Security.Authenticated(Secured.class)
    public static Result uploadJar() {
        String username = session().get("username");
        User user = User.getUserByName(username);
        MultipartFormData body = request().body().asMultipartFormData();
        FilePart jar = body.getFile("jar");
        if (jar != null) {
            String fileName = jar.getFilename();
            String contentType = jar.getContentType();
            File file = jar.getFile();
            try {
                if (GameController.isValidJar(file))
                {
                    FileInputStream fis = new FileInputStream(file);
                    File savedFile = new File(JAR_FILE_LOCATION + file.separator + username + '_' + user.jarVersion + ".jar");
                    FileOutputStream fos = new FileOutputStream(savedFile);
                    FileUtils.copy(fis, fos);
                }
                else
                {
                    System.out.println("Failed to load the jar file");
                }
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return ok("File uploaded");
        }
        else {
            flash("error", "Missing file");
            return redirect(routes.Application.userMain());
        }
    }

    @Security.Authenticated(Secured.class)
    public static Result runGame()
    {
        try
        {
            Class oneClass = GameController.getPlayerClass(new File("storage/jars/lion.jar"));
            Player one = (Player)oneClass.newInstance();
            one.initialize(1);

            Class twoClass = GameController.getPlayerClass(new File("storage/jars/sample.jar"));
            Player two = (Player)twoClass.newInstance();
            two.initialize(2);

            Class threeClass = GameController.getPlayerClass(new File("storage/jars/woodsman.jar"));
            Player three = (Player)threeClass.newInstance();
            three.initialize(3);

            GameController.RunGame(one, two, three);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        return ok("We ran a game");
    }

    public static Result viewGame(Long id) {
        return TODO;
    }

    public static Result deleteUser(Long id) {
        User.delete(id);
        return redirect(routes.Application.users());
    }

    public static class Login {
        @Required
        public String username;

        @Required
        public String password;

        public void setUsername(String username)
        {
            this.username = username;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public String validate() {
            if (!User.authenticate(username, password)) {
                return "Invalid user or password";
            }
            return null;
        }
    }

    public static class AdminLogin {
        @Required
        public String username;

        @Required
        public String password;

        public void setUsername(String username)
        {
            this.username = username;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public String validate() {
            if (!username.equals("walter"))
            {
                return "Invalid user or password";
            }
            if (!password.equals("corerul3z"))
            {
                return "Invalid user or password";
            }
            return null;
        }
    }
  
}