/*
* Copyright 1999-2004 The Apache Software Foundation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
/*
    CVS: $Id: prefs.js,v 1.5 2004/03/10 10:18:53 cziegeler Exp $

    This file is the central controller piece in the preferences
    application. It receives the requests from the client browser (the
    View in the MVC pattern), and coordinates with the Model (written
    in Java, in the logic/ directory) to do the business logic.

    Author: Ovidiu Predescu <ovidiu@apache.org>
    Date: August 30, 2002
 */

// The global user registry, through which we add new users or access
// existing ones.
var userRegistry = Packages.org.apache.cocoon.samples.flow.prefs.UserRegistry.getUserRegistry();

var user;

// This top-level function is called from the sitemap to start the
// process of registering a user.
// 
// This function takes care of all the steps required during the
// registration of a new user. It is the controller is the MVC
// pattern, an intermediary between the View, in our case the client's
// browser, and the Model, in the case of this particular function the
// code that maintains the registered users.
//
// The function collects the necessary information from the View, and
// calls the Model to do the necessary work of checking whether the
// user exists or not, and registering the new user.
function registerUser()
{
  var check = false;
  var errorMsg = null;
  
  var login = "";
  var password = "";
  var firstName = "";
  var lastName = "";
  var email = "";

  while (true) {
    // Present the user with addUser page. `check' indicates the XSP
    // template that it needs to check the registration parameters,
    // and print an indicator close to where the errors
    // are. `errorMsg' if not null is printed at the top of the page
    // as an error message.
    cocoon.sendPageAndWait("page/userInfo",
                    { 
                        "check" : check, 
                        "errorMsg" : errorMsg,
                        "title": "New User Registration",
                        "button" : "Register",
                        "login" : login, 
                        "password" : password,
                        "firstName" : firstName, 
                        "lastName" : lastName,
                        "email" : email
                     }
    );

    check = false;
    errorMsg = null;

    login = cocoon.request.login;
    password = cocoon.request.password;
    firstName = cocoon.request.firstName;
    lastName = cocoon.request.lastName;
    email = cocoon.request.email;

    if (login == "" || password == ""
        || firstName == "" || lastName == "" || email == "") {
      check = true;
      errorMsg = "Please correct the marked errors before continuing";
      continue;
    }

    // Check for the existence of an already registered user with the
    // same login name. There is a possibility of a race condition
    // here, with another user registering with the same login id
    // between this check and the creation of a new user few lines
    // below. We ignore this problem in this example.
    var existingUser = userRegistry.isLoginNameTaken(login);
    if (!existingUser) {
      user = new Packages.org.apache.cocoon.samples.flow.prefs.User(login, password,
                                                                    firstName, lastName,
                                                                    email);
      userRegistry.addUser(user);
      break;
    } else {
      errorMsg = "Login name '" + login
        + "' is already in use, please choose another name";
    }
  }

  // The user has successfully registered, so we consider (s)he to be
  // already logged in. At this point we want to create a session, so
  // that all the JavaScript global variables are shared among
  // invocations of top-level functions. Up until this point, each
  // invocation had a separate global scope for the variables.
  var session = cocoon.session;

  // Here we just send a response to the client browser and we don't
  // wait for any response. This is the last page generated by this
  // top-level function. In general is good to make sure a top-level
  // function, e.g. one that's invoked directly from the sitemap using
  // <map:call function="..."> sends a response page to the client at
  // all the exit points. Otherwise the user will get a blank page and
  // will be really confused.
  //
  // In the case of this particular function, this is the only exit
  // point.
  cocoon.sendPage("page/registrationSuccessful", {"user" : user});
}


// This top-level function is used for user login.
function login(errorMsg)
{
  var login = "";
  var password = "";

  while (true) {
    cocoon.sendPageAndWait("page/login",
                {
                    "errorMsg" : errorMsg, 
                    "login" : login, 
                    "password" : password
                }
    );

    errorMsg = null;
  
    login = cocoon.request.getParameter("login");
    password = cocoon.request.getParameter("password");

    user = userRegistry.getUserWithLogin(login, password);

    if (user != undefined) {
      break;
    } else {
      errorMsg = "No such user or bad password";
    }
  }

  // The user has successfully signed in. At this point we want to
  // create a session, so that all the JavaScript global variables are
  // shared among invocations of top-level functions. Up until this
  // point, each invocation had a separate global scope for the
  // variables.
  var session = cocoon.session;

  // We send to the user a welcome page which contains links back to
  // what (s)he can do. These links essentially point to other top
  // level functions in this script.
  cocoon.sendPage("page/welcome", {"user" : user});
}

// This function is called to edit the preferences of an already
// registered user. If this function was called without the user being
// logged in first, the 'user' global variable is null. When this
// happens the user is redirected to the login page first.
function edit()
{
  if (user == undefined)
    login("Please login before continuing");

  var login = user.login;
  var password = user.password;
  var firstName = user.firstName;
  var lastName = user.lastName;
  var email = user.email;
  var errorMsg = "";
  var check = false;

  while (true) {
    // Present the user with addUser page. `check' indicates the XSP
    // template that it needs to check the registration parameters,
    // and print an indicator close to where the errors
    // are. `errorMsg' if not null is printed at the top of the page
    // as an error message.
    cocoon.sendPageAndWait("page/userInfo",
                    { 
                        "check" : check, 
                        "errorMsg" : errorMsg,
                        "title": "Edit account",
                        "button" : "Change", 
                        "cancel" : true,
                        "login" : login, 
                        "password" : password,
                        "firstName" : firstName, 
                        "lastName" : lastName,
                        "email" : email
                     }
    );

    if (cocoon.request.get("cancel"))
      break;

    check = false;
    errorMsg = null;

    login = cocoon.request.get("login");
    password = cocoon.request.get("password");
    firstName = cocoon.request.get("firstName");
    lastName = cocoon.request.get("lastName");
    email = cocoon.request.get("email");

    if (login == "" || password == ""
        || firstName == "" || lastName == "" || email == "") {
      check = true;
      errorMsg = "Please correct the marked errors before continuing";
      continue;
    } else {
      // Save the changes the user made in the User object
      user.login = login;
      user.password = password;
      user.firstName = firstName;
      user.lastName = lastName;
      user.email = email;
      break;
    }
  }

  cocoon.sendPage("page/welcome", {"user" : user});
}

function logout()
{
  user = undefined;
  login("You're successfully logged out. Please log in to continue");
}
