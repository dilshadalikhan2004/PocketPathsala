(function(){
  "use strict";
  const $=id=>document.getElementById(id), state={health:null,lastQuestion:null,source:null};
  const setStatus=(text,kind)=>{const el=$("status");el.textContent=text;el.className="status "+(kind||"");};
  const setConnection=(text,kind)=>{$("connection").textContent=text;$("connection").className="pill "+kind;};
  async function api(path,options){const r=await fetch(path,options);let data={};try{data=await r.json()}catch(_){}
    if(!r.ok) throw new Error(data.message||data.error||"Request failed"); return data;}
  async function connect(){try{
    state.health=await api("/api/health"); setConnection("Connected","online");
    const chapters=await api("/api/chapters"), select=$("chapter"); select.textContent="";
    const all=document.createElement("option");all.value="__all__";all.textContent="All chapters";select.appendChild(all);
    chapters.forEach(c=>{const o=document.createElement("option");o.value=c.id;o.textContent=c.name;select.appendChild(o)});
    setStatus((state.health.modelProvider||"Local model")+" · "+chapters.length+" chapters ready");
  }catch(e){setConnection("Offline","offline");setStatus(e.message+" Retry when the host is ready.","error");$("retry").hidden=false;}}
  function clearAnswer(){ $("answer-panel").hidden=false;$("answer").textContent="";$("citations").textContent="";$("evidence").textContent="";$("provider").textContent=""; }
  function card(parent,title,text){const el=document.createElement("div");el.className="card";const strong=document.createElement("strong");strong.textContent=title;el.append(strong);const p=document.createElement("span");p.textContent=text;el.append(p);parent.append(el)}
  function handleEvent(evt){let data;try{data=JSON.parse(evt.data)}catch(_){return}
    if(evt.type==="queued")setStatus("Queued at position "+data.position+"…","busy");
    else if(evt.type==="status")setStatus(data.value==="generating"?"Writing a grounded answer…":"Finding evidence…","busy");
    else if(evt.type==="token")$("answer").textContent+=data.value;
    else if(evt.type==="citation")card($("citations"),"Source",data.value);
    else if(evt.type==="evidence"){const p=document.createElement("p");p.textContent=data.chunk&&data.chunk.text||data.text||"";$("evidence").append(p)}
    else if(evt.type==="done"){$("provider").textContent=data.provider||"Local";setStatus("Answer complete","")}
    else if(evt.type==="error"){setStatus(data.message||data.code||"Request failed","error");$("retry").hidden=false;}
  }
  async function ask(){const q=$("question").value.trim();if(!q){setStatus("Write a question first.","error");return}
    if(!$("chapter").value){setStatus("Choose a chapter.","error");return} clearAnswer(); $("ask").disabled=true;$("retry").hidden=true;
    state.lastQuestion={chapterId:$("chapter").value==="__all__"?null:$("chapter").value,question:q,language:$("language").value,answerMode:$("mode").value};
    try{const accepted=await api("/api/ask",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(state.lastQuestion)});
      const response=await fetch("/api/ask/"+encodeURIComponent(accepted.requestId)+"/events"); if(!response.ok||!response.body)throw new Error("Streaming connection failed");
      const reader=response.body.getReader(),decoder=new TextDecoder(),buffer="";
      // The host sends standard SSE frames; parse complete blank-line-delimited frames.
      let pending=buffer;
      while(true){const part=await reader.read();if(part.done)break;pending+=decoder.decode(part.value,{stream:true});
        const frames=pending.split(/\r?\n\r?\n/);pending=frames.pop();frames.forEach(frame=>{const lines=frame.split(/\r?\n/),e={type:"message",data:""};lines.forEach(line=>{if(line.indexOf("event: ")===0)e.type=line.slice(7);if(line.indexOf("data: ")===0)e.data+=line.slice(6)});if(e.data)handleEvent(e)})}
    }catch(e){setStatus(e.message,"error");$("retry").hidden=false}finally{$("ask").disabled=false}
  }
  async function quiz(){const chapter=$("chapter").value;if(!chapter){setStatus("Choose a chapter for the quiz.","error");return}
    try{const data=await api("/api/quizzes/"+encodeURIComponent(chapter));$("quiz-panel").hidden=false;$("quiz-title").textContent=data.title||"Cached quiz";const list=$("quiz-list");list.textContent="";
      data.questions.forEach((q,i)=>{const wrap=document.createElement("article");wrap.className="quiz-question";const h=document.createElement("h3");h.textContent=(i+1)+". "+q.question;wrap.append(h);const opts=document.createElement("div");opts.className="options";
        q.options.forEach(option=>{const b=document.createElement("button");b.textContent=option;b.onclick=async()=>{if(b.disabled)return;try{const result=await api("/api/quizzes/"+encodeURIComponent(q.id)+"/answer",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({questionId:q.id,answer:option})});b.classList.add(result.correct?"correct":"wrong");opts.querySelectorAll("button").forEach(x=>x.disabled=true);const f=document.createElement("p");f.className="feedback";f.textContent=(result.correct?"Correct. ":"Not quite. ")+result.explanation;wrap.append(f)}catch(e){setStatus(e.message,"error")}};opts.append(b)});wrap.append(opts);list.append(wrap)})
    }catch(e){setStatus(e.message,"error")}
  }
  $("ask").onclick=ask;$("retry").onclick=()=>{if(state.lastQuestion)ask();else connect()};$("quiz").onclick=quiz;connect();
})();
