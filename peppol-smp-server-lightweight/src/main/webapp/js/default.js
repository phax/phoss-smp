/*
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
function AppClass(){}
AppClass.prototype = {
  /**
   * Perform a login via AJAX
   * @param ajaxUrl AJAX URL to use
   * @param vals custom data like login name and password
   * @param errorField The ID of the HTML element that will retrieve the error message
   */
  viewLogin : function(ajaxUrl,vals,errorField) {
    $.ajax ({
      type: 'POST',
      url:ajaxUrl,
      data:vals,
      success:function(data){
        if (data.value.loggedin) {
          // reload the whole page because of too many changes
          location.reload ();
        }
        else
          $('#'+errorField).empty ().append (data.value.html);
      }
    });
  }
};

var App = window.App = new AppClass();
