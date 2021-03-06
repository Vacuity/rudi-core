<html>
<head>
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>
	<style>
		input:focus,
		select:focus,
		textarea:focus,
		button:focus {
		    outline: none;
		}
.css-typing
{
    width: 30em;
    white-space:nowrap;
    overflow:hidden;
    -webkit-animation: type 5s steps(50, end);
    animation: type 5s steps(50, end);
}

@keyframes type{
    from { width: 0; }
}

@-webkit-keyframes type{
    from { width: 0; }
}
body{
	background-color: #114363; //#18608f; //#2980b9;
}



.spinner {
  width: 40px;
  height: 40px;

  position: relative;
  margin: 100px auto;
}

.double-bounce1, .double-bounce2 {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: #fff;
  opacity: 0.6;
  position: absolute;
  top: 0;
  left: 0;
  
  -webkit-animation: sk-bounce 2.0s infinite ease-in-out;
  animation: sk-bounce 2.0s infinite ease-in-out;
}

.double-bounce2 {
  -webkit-animation-delay: -1.0s;
  animation-delay: -1.0s;
}

@-webkit-keyframes sk-bounce {
  0%, 100% { -webkit-transform: scale(0.0) }
  50% { -webkit-transform: scale(1.0) }
}

@keyframes sk-bounce {
  0%, 100% { 
    transform: scale(0.0);
    -webkit-transform: scale(0.0);
  } 50% { 
    transform: scale(1.0);
    -webkit-transform: scale(1.0);
  }
}

.response , .response a{
	background-color:transparent; 
	color:#ffffff; 
	font-family: 'Droid Serif', serif; 
}
.response a{
	text-decoration:underline;
	color: #CDEB8B;
}

	</style>
	<link href="https://fonts.googleapis.com/css?family=Droid+Serif" rel="stylesheet">
	<script lang="javascript">
		function noCursor(a){
		  var a = document.getElementById(a),
		      b = document.createElement('input');
		  b.setAttribute("style","position: absolute; right: 101%;");
		  a.parentNode.insertBefore(b, a);

		  if(a.addEventListener){
		    b.addEventListener("input",function(){a.value = b.value});
		    a.addEventListener("focus",function(){b.focus()});
		  }else{
		    a.attachEvent("onfocus",function(){b.focus()});
		    b.attachEvent("onpropertychange",function(){a.value = b.value});
		  };
		 
		}
		$(document).bind('keydown',function(e){
			guaranteeInput();
			if ( $( "#msg" ).length ) {
		        $('#msg').remove();
			}
			if ( $( "#channel" ).length ) {
		        $('#channel').remove();
			}
			if ( $( "#duration" ).length ) {
		        $('#duration').remove();
			}
			if ( $( "#logs" ).length ) {
		        $('#logs').remove();
			}
			if ( $( "#link" ).length ) {
		        $('#link').remove();
			}
			while ( $( "#label" ).length ) {
		        $('#label').remove();
			}
			while ( $( "#score" ).length ) {
		        $('#score').remove();
			}
			while ( $( "#duration" ).length ) {
		        $('#duration').remove();
			}
		    if (e.which == "13") {
		    	getResponse();
		        /**/
		        //enter pressed 
		        
		    }
		    else {
		    	doSpotlight();
		    }
		    if ($(':focus:not("input")').length){
		    }
		});
		
		function guaranteeInput(){
			if ( $( "#inp" ).length <= 0 ) {
				$("body").empty();
			    d = document.createElement('input');
		       	$(d).css({
		       			backgroundColor:"transparent", 
		       			color:"#ffffff", 
		       			fontFamily: "Droid Serif', serif", 
		       			border:"0px solid transparent",
		       			fontSize:"40pt",
		       			width: "100%"
		       		})
		        	.attr("type","text")
		        	.attr("id","inp")
		        	.attr("width","100")
		        	.attr("height","100")
		        	.attr("autocomplete","off")
	        	    .appendTo($("body")); //main div		       		
		        noCursor('inp');
			}	        
	        $('#inp').focus();
		}
		
		function getResponse(){	        
	        query = $('#inp').val();
		    $('#inp').remove();
		    
	        d = document.createElement('div');
        	$(d).addClass('spinner')
	        	.attr("id","spinner")
        	    .html("<div class='double-bounce1'></div><div class='double-bounce2'></div>")
        	    .appendTo($("body")); //main div

	        $.getJSON( "json?d=true&q=" + query, function( data ) {
	        	
	        	
	        	$.each(data.logs, function(i, item) {
		        	d = document.createElement('div');
		        	$(d).addClass('response')
		        	    .html("> " + item)
			        	.attr("id","logs")
		        	    .appendTo($("body")); //main div
	        	});
        	    	        	
	        	d = document.createElement('div');
	        	$(d).addClass('response')
	        	    .html("> Total match time: " + data.duration + " seconds")
		        	.attr("id","duration")
	        	    .appendTo($("body")); //main div
	        	
	        	d = document.createElement('div');
	        	$(d).addClass('response')
	        	    .html("> Channel Id: <a href='"+data.channelId+"'>" + data.channelId + "</a>")
		        	.attr("id","channel")
	        	    .appendTo($("body")); //main div
	        	    
		        d = document.createElement('div');
	        	$(d).addClass('response')
		        	.attr("id","link")
	        	    .html("> Explore the <a href='" + data.link + "'>Index</a> for data linked to your channel id.")
	        	    .appendTo($("body")); //main div
		        	    
		        	    
	        	ready(data.msg);
		        	    
		        $('#spinner').remove();
	        });
		}
		
		function doSpotlight(){	        
	        query = $('#inp').val();
		    
		    
	        $.getJSON( "json?q=" + query, function( data ) {
	        	
 	        	$.each(data.matches, function(i, match) {
 	        		
		        	$.each(match.labels, function(j, l) {
		        		d = document.createElement('div');		        	
			        	$(d).addClass('response')
			        		.html("> Label[" + i + "." + j + "]: " + l)
				        	.attr("id","label")
			        	    .appendTo($("body")); 
		        	});

		        	  
	        		d = document.createElement('div');		        	
		        	$(d).addClass('response')
		        		.html("> Score[" + i + "]: " + match.score)
		        		.attr("id","score")
	        	    	.appendTo($("body")); 
		        	
 	        		 
 	        	});
		        	    
	        	
	        	
	        	d = document.createElement('div');
	        	$(d).addClass('response')
	        	    .html("> Total match time: " + data.duration + " seconds")
		        	.attr("id","duration")
	        	    .appendTo($("body")); //main div
	        	
		        	    
		        
	        });
		}
		
		function load(){
        	query = "${query}";
			if(query){
				$("body").empty();
				guaranteeInput();
				$('#inp').val(query);
				getResponse();
			}
			else ready("Go ahead, I'm listening.");
		}
		
		function ready(msg){
		    d = document.createElement('div');
	       	$(d).addClass('css-typing')
	       		.css({
	       			fontFamily: "'Droid Serif', serif'",
					color: "#ffffff",
					fontSize: "16pt",
					paddingTop: "1em"
				})
	        	.attr("id","msg")
	        	.html(msg)
        	    .appendTo($("body")); //main div
		}
		
	</script>
</head>
	<body onload="load()"> 
	<!-- 
	<div class="css-typing" style="color:#ffffff; font-family: 'Droid Serif', serif'; font-size:40pt" id="msg">Start typing.</div>
	<div id="spinner" style="display:none;" class="spinner">
  		<div class="double-bounce1"></div>
  		<div class="double-bounce2"></div>
	</div>
	-->
<!-- 	<input type="text" style="background-color:transparent; color:#ffffff; font-family: 'Droid Serif', serif; border:0px solid transparent;font-size:40pt" width=100 height=100 id="inp" autocomplete="off"/> -->
	</body>
</html>