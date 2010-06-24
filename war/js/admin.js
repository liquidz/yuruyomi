(function(){
	$(function(){
		//$("table").colorize();
		$("a.warn").bind("click", function(e){
			return window.confirm("move to [" + $(e.target).attr("href") + "]. ok?");
		});
	});
})();


