(ns see.core
  (:import
    (javax.swing WindowConstants JFrame)
    (java.awt Dimension Graphics Image Color Graphics2D)
    (java.util.concurrent Executors TimeUnit)
    (java.awt.event WindowAdapter)))


(defn definition-for [image]
  {:image image
   :title "See image"
   :background-colour (Color. 0 0 0 0)
   :fps 25
   :only-draw-when-updated? false})

(defn with-title [see-definition title]
  (assoc see-definition :title title))

(defn with-background-colour [see-definition background-colour]
  (assoc see-definition :background-colour background-colour))

(defn with-frames-per-second [see-definition frames-per-second]
  (assoc see-definition :fps frames-per-second))

(defn with-no-redraws-unless-updated [see-definition]
  (assoc see-definition :only-draw-when-updated? true))

(defn see
  "See a visual representation of an image in a java.awt Window.
  Returns a function to call when the image has been changed.

  Example usage:

  (require '[see :as s])

  (-> (s/definition-for my-java-awt-image)
      (s/with-title \"My Image\")
      (s/with-background-colour Color/YELLOW)
      (s/see))"
  [{:keys [^String title
           ^Image image
           ^Color background-colour
           ^Long fps
           ^Boolean only-draw-when-updated?]}]
  (let [frame ^JFrame (proxy [JFrame] []
                        (paint [^Graphics graphics]
                          (let [insets (-> this .getInsets)
                                container (-> this .getContentPane)]
                            (.setBackground ^Graphics2D graphics background-colour)
                            (.clearRect graphics
                                        (.left insets) (.top insets)
                                        (.getWidth container) (.getHeight container))
                            (.drawImage graphics image (.left insets) (.top insets) this))))
        changed? (volatile! true)
        executor (Executors/newSingleThreadScheduledExecutor)]
    (.scheduleAtFixedRate executor
                          #(when (or @changed? (not only-draw-when-updated?))
                             (vreset! changed? false)
                             (.repaint frame))
                          0 (long (/ 1000 fps)) TimeUnit/MILLISECONDS)
    (doto frame
      (.setTitle title)
      (-> .getContentPane (.setPreferredSize
                            (Dimension. (.getWidth image nil)
                                        (.getHeight image nil))))
      (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE)
      (.addWindowListener (proxy [WindowAdapter] []
                            (windowClosed [_window-event]
                              (.shutdown executor))))
      (.pack)
      (.setVisible true))
    #(vreset! changed? true)))
