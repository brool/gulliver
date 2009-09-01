(ns gulliver
  (:use compojure)
  (:import java.io.File))

(defn- transform-string
  "Internal.  Transform a string into the appropriate echo statement.
An empty string translates into a single space."  
  [s]
  (if (> (.length s) 0)
    (format "(gulliver/echo %s)" (pr-str s))
  " "))

(defn- transform-code
  "Internal.  Transform code so that it can run as a render routine."
  [s]
  (if (= (first s) \=)
    (format "(gulliver/echo %s)" (.substring s 1))
    s))

(defn- convert* 
  "Internal.  Given a string that represents a Clojure template,
produce a flat list of the statements necessary to produce that template."
  [s]
  (let [m (re-matcher #"(?s)(.*?)<\?(.+?)\?>|(.+?)$" s)]
    (loop [next (re-find m) buffer ["'("]]
      (cond
       (not (nil? (nth next 3))) 
           (eval (read-string (apply str (conj buffer (transform-string (nth next 3)) ")" ))))
       :otherwise (recur 
                   (re-find m) 
                   (conj buffer 
                         (transform-string (nth next 1)) 
                         (transform-code (nth next 2))))
))))


(defn- strip-imports 
  "Internal. Given a list of statements, separate them into ones that
set up the compilation context and the code itself."
  [statements]
  (let [f (fn [x] (#{ 'import 'use 'ns } (first x)))]
    (split-with f statements)))

(defn convert 
  "Given a namespace and a string containing a template, create the
  namespace with a function called 'render' that implements the given
  page."
  [new-ns s]
  (let [codelist (convert* s)
        [env code] (strip-imports codelist)]
    (binding [*ns* *ns*]
       (eval `(ns ~(symbol new-ns)))
       (eval `(do ~@env))
       (eval `(defn ~'render [~'request] ~@codelist))
       (eval `~'render)
)))

; this holds the uri -> function mappings
(def function-cache (ref {}))

; generic function that echos to the buffer
(defn echo 
  "Generic function that echoes to the output buffer."
  [&args])

(defn- filename-to-namespace 
  "Convert a filename into the equivalent namespace"
  [fn]
  (.replace (if (= (first fn) \/) (.substring fn 1) fn) "/" "."))

; the default servlet
(defroutes template-servlet
  (ANY "/*" 
       ; try to load the file with the given URI
       (let [uri (request :uri)
             filename (str "." (request :uri))
             f (File. filename)
             file-modified (.lastModified f)
             [cache-modified code] (@function-cache uri)]

         ; if no cache entry or it's been modified, rebuild it
         (when (or 
                (nil? cache-modified) 
                (not= file-modified cache-modified))
           (dosync
            (commute function-cache assoc uri 
                     [file-modified (convert (filename-to-namespace filename) (slurp filename))])))

         ; guaranteed to have it now -- invoke it and return the buffer
         (let [buffer (atom [])]
           (binding [echo (fn [s] (swap! buffer conj (str s)))]
             ((nth (@function-cache uri) 1) request)
             @buffer
             ))
)))
       
;; (defserver web-server
;;   {:port 8080}
;;   "/*"  (servlet template-servlet))
