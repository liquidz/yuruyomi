var getImage = function(id){
	$.get("/ajax/getimage", {id: id}, function(data){
		if(data !== "nil") $("#box" + id).html("<img src='" + data + "' />");
	});
};

(function(){
 	Yuruyomi = {};

	Yuruyomi.mainBookTypes = ["reading", "want", "have"];
	Yuruyomi.noBookClass = ".no_book_msg";
	Yuruyomi.fadeSpeed = 500;

	Yuruyomi.largeImageSize = 350;

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

	//Yuruyomi.changeBooks = function(e){
	Yuruyomi.changeBooks = function(target){
		//var target = $(e.target);

		var type = Yuruyomi.getInfoType();
		var newType = Yuruyomi.getInfoType(target);

		if(newType === "all"){
			$.each(Yuruyomi.mainBookTypes, function(){
				if((""+this) !== type) Yuruyomi.showBooks(this);
			});
			$("#container " + Yuruyomi.noBookClass).hide();
		} else {
			if(type === "all"){
				$.each(Yuruyomi.mainBookTypes, function(){
					if((""+this) !== newType) Yuruyomi.hideBooks(this);
				});
				$("#container ." + newType).filter(Yuruyomi.noBookClass).fadeIn(Yuruyomi.fadeSpeed);
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

	jQuery.event.add(window, "load", function(){
		var lbi = $("#large_book_image img");
		if(lbi && lbi.height() > Yuruyomi.largeImageSize){
			//lbi.attr("original_height", lbi.height());
			//lbi.attr("original_width", lbi.width());
			lbi.height(Yuruyomi.largeImageSize);

			/*
			lbi.hover(function(){
				var obj = $(this);
				obj.animate({
					width: parseInt(obj.attr("original_width")),
					height: parseInt(obj.attr("original_height"))
				}, 250);
			}, function(){
				var obj = $(this);
				var nh = parseInt(obj.attr("original_height"));
				var nw = parseInt(obj.attr("original_width"))
				var sw = Yuruyomi.largeImageSize * nw / nh;
				obj.animate({width: sw, height: Yuruyomi.largeImageSize}, 250);
			});
			*/
		}
	});

	$(function(){
		// タイトル、著者のホバー表示
		$(".book").hover(function(){
			$(this).find(".book_info").fadeIn(250);
		}, function(){
			$(this).find(".book_info").fadeOut(250);
		});

		// QRコード生成
		if($("#show_qr_code").length !== 0){
			var mobileUrl = encodeURIComponent(location.href.replace(/\/user/, "/m"));
			var gcUrl = "http://chart.apis.google.com/chart?cht=qr&chs=110x160&chl=" + mobileUrl;
			$("#container").append($("<div class='book'><div class='book_image'><img src='" + gcUrl + "' /></div></div>"));
		}

		$("form#new-book-form").bind("submit", function(e){
			return window.confirm("本当に" + $("form#new-book-form input.confirm").val() + "して良いですか？");
		});

		// 画像読み込み
		var books = $("div.book img.load");
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
		setTimeout(loadImage, 0);
	});
})();
