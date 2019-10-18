Just for documentation. This is called during a refresh in Forms:
Original Code is in data_modeler.fmb

PROCEDURE p_refresh IS
BEGIN
  :system.message_level:=25;
  commit; 
  :system.message_level:=0;

 
	declare
		mc1 varchar2(4000);
		mc2 varchar2(4000);
		mboxes varchar2(4000);
		mlinks varchar2(4000);
		
	begin
		mc1:='
 
		<!DOCTYPE html>
  	<html>
	 <head>
      <link rel="stylesheet" type="text/css" href="https://forms-demo.com/joint.css" />
   </head>
   <body>
      <!-- content -->
      <div id="myholder"></div>
      <!-- dependencies -->
      <script src="https://forms-demo.com/jquery.js"></script>
      <script src="https://forms-demo.com/lodash.js"></script>
      <script src="https://forms-demo.com/backbone.js"></script>
      <script src="https://forms-demo.com/joint.js"></script>
      <!-- code -->
      <script type="text/javascript">
         var graph = new joint.dia.Graph();
         var paper = new joint.dia.Paper({
         el: document.getElementById(''myholder''),
         width: 800,
         height: 600,
         gridSize: 1,
         model: graph,
         perpendicularLinks: true,
         restrictTranslate: true
         });
         
         var member = function(x, y, rank, name, image, background, textColor,p_mid) {
         
         textColor = textColor || "#000";
         
         var cell = new joint.shapes.org.Member({
         position: { x: x, y: y },
         attrs: {
             ''.card'': { fill: background, stroke: ''none''},
             ''mID'': 0 ,
               image: { ''xlink:href'':  image, opacity: 0.7 },
             ''.rank'': { text: rank, fill: textColor, ''word-spacing'': ''-5px'', ''letter-spacing'': 0},
             ''.name'': { text: name, fill: textColor, ''font-size'': 13, ''font-family'': ''Arial'', ''letter-spacing'': 0 }
         }
         });
         
         paper.on(''cell:pointerup'', function(cellView, evt, x ,y ) {
					   if (cellView.model.attributes.type ==''org.Member''){
					    var x = cellView.model.position().x;
					    var y = cellView.model.position().y;
							js1(''A,''+cellView.model.attr().mID+'',''+x+'',''+y); } 
					});
					
         graph.addCell(cell);
         return cell;
         };
         
         function link(source, target, breakpoints,p_id) {
         
         var cell = new joint.shapes.org.Arrow({
         source: { id: source.id },
         target: { id: target.id },
         vertices: breakpoints,
         attrs: {
             ''.connection'': {
                 ''fill'': ''none'',
                 ''mID'': 0 ,
                 ''stroke-linejoin'': ''round'',
                 ''stroke-width'': ''2'',
                 ''stroke'': ''#4b4a67''
             }
         } 
         });
         cell.attr().mID=p_id;
       	 paper.on(''link:pointerup'', function(cellView, evt, x ,y ) {
					    if (cellView.model.attributes.type ==''org.Arrow''){ 
								js1(''B,''+cellView.model.attr().mID+'',''+JSON.stringify(cellView.model.vertices()) ); } 
				 });
         graph.addCell(cell);
         return cell;
         }
         
         function js1(p1){
						window.myCallBack.mf1(p1);
						}
         '
         ;
         mc2:=
         '         
	      </script>
	   </body>
	</html>
	
	';
	  
	 for i in ( select V1, V2,V3, V4, V5 ,V6, V7,v8 from sc_errors where v1 = 'DIAG') loop
	 	    if i.v7 = 'M' then
			 	    mboxes:=mboxes||' 
			 	  	  var mb'||i.v2||' = member('||i.v5||', '||i.v6||', '''||i.v3||'('||i.v2||')'','''||i.v4||''', ''https://forms-demo.com/male-icon-png-22.png'', ''#7c68fd'', ''#f1f1f1'','||i.v2||');
			 	  	  mb'||i.v2||'.attr().mID='||i.v2||';
						 	';
	 	    else
	 	    	 mboxes:=mboxes||' 
	 	    			var mb'||i.v2||' = member('||i.v5||', '||i.v6||', '''||i.v3||'('||i.v2||')'','''||i.v4||''', ''https://forms-demo.com/female-employee-icon-8.png'', ''#feb563'','||i.v2||');
						  			 	  	  mb'||i.v2||'.attr().mID='||i.v2||';';
	 	    end if;
	 	     
	 end loop;
	 
	 for i in ( select V1, V2,V3, V4, V5 ,V6, V7,v8 from sc_errors where v1 = 'DIAG_LINKS') loop
	 	    if i.v4  IS NOT NULL AND i.v5  IS NOT NULL AND i.v6  IS NOT NULL AND i.v7  IS NOT NULL   then
			 	    mlinks:=mlinks||' 
							link(mb'||i.v2||', mb'||i.v3||', [{x: '||i.v4||', y: '||i.v5||'}, {x: '||i.v6||', y: '||i.v7||'}],'||i.v8||');						 	';
	 	    elsif i.v4  IS NOT NULL AND i.v5  IS NOT NULL then
	 	    	 mlinks:=mlinks||' 
  		       link(mb'||i.v2||', mb'||i.v3||', [{x: '||i.v4||', y: '||i.v5||'}],'||i.v8||');
						  	';
	 	    end if;
	 	     
	 end loop;
	   
	 set_custom_property('BL.JB', 1, 'SET_CONTENT',mc1||  mboxes ||  ' ' || mlinks||  ' ' || mc2 );