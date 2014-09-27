angular.module("Common", []).factory("jsonFactory", function() {

   return {
      createShortcut : function() {
         return [ {
            "id" : 1,
            "name" : "Career Analytics",
            "keyShort" : "Ctrl + Alt + A"
         }, {
            "id" : 2,
            "name" : "Find Job",
            "keyShort" : "Ctrl + Alt + F"
         }, {
            "id" : 3,
            "name" : "Function Name 1",
            "keyShort" : "Ctrl + Alt + 1"
         }, {
            "id" : 4,
            "name" : "Function Name 2",
            "keyShort" : "Ctrl + Alt + 2"
         } ];
      }
   }
});