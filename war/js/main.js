var getImage = function(id){
	$.get("/ajax/getimage", {id: id}, function(data){
		if(data !== "nil") $("#box" + id).html("<img src='" + data + "' />");
	});
};

(function(){
 	Yuruyomi = {};

	Yuruyomi.mainBookTypes = ["reading", "want", "have"];
	Yuruyomi.fadeSpeed = 500;

	Yuruyomi.getInfoType = function(obj){
		var target = obj ? obj : $("#info ul li a.selected");
		return target.attr("id").split("_")[0];
	};

	// book controller {{{
	Yuruyomi.showBooks = function(type, fn){
		var target = $("#container ." + type);
		if(target.length === 0 && fn !== undefined){
			fn();
		} else {
			target.fadeIn(Yuruyomi.fadeSpeed, function(){
				if(fn !== undefined) fn();
			});
		}
	};
	Yuruyomi.hideBooks = function(type, fn){
		var target = $("#container ." + type);
		if(target.length === 0 && fn !== undefined){
			fn();
		} else {
			target.fadeOut(Yuruyomi.fadeSpeed, function(){
				if(fn !== undefined) fn();
			});
		}
	};

	Yuruyomi.changeBooks = function(e){
		var target = $(e.target);

		var type = Yuruyomi.getInfoType();
		var newType = Yuruyomi.getInfoType(target);
		if(newType === "all"){
			$.each(Yuruyomi.mainBookTypes, function(){
				if((""+this) !== type) Yuruyomi.showBooks(this);
			});
		} else {
			if(type === "all"){
				$.each(Yuruyomi.mainBookTypes, function(){
					if((""+this) !== newType) Yuruyomi.hideBooks(this);
				});
			} else {
				Yuruyomi.hideBooks(type, function(){
					Yuruyomi.showBooks(newType);
				});
			}
		}

		// toggle "selected"
		$("#info ul li a.selected").removeClass("selected");
		target.addClass("selected");
		
		return false;
	};
	// }}}

	$(function(){
		$("#info ul.main li a").bind("click", Yuruyomi.changeBooks);

		var books = $("div.book img");
		var i = 0, l = books.length;
		var loadImage = function(){
			if(i >= l) return;

			var target = $(books.get(i));
			$.get("/ajax/getimage", {id: target.attr("id")}, function(data){
				target.attr("src", data);
				++i;
				setTimeout(loadImage, 1000);
			});
		};
		setTimeout(loadImage, 500);
	});
})();
