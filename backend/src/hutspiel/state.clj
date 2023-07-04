(ns hutspiel.state
  (:require [clojure.spec.alpha :as spec]))

(spec/def :uuid/public uuid?)
(spec/def :uuid/private uuid?)

(defn uuid [] (java.util.UUID/randomUUID))

; Questions:
; - Auxilary datastructures to improve perf?
;   Probably not needed here, just interesting in general.
; - Communicate changes to frontend? Send diff? Idea from Scattered Thoughts post?
; - Nested modifications? Zip library? Probably not needed.
; - Performance of putting all games in a single atom map with a high update freq?
; - How to remove an unused game?

; TODO:
; - check distinct player / team names
; - check before game starts
;   - if players even: that all teams have equal number of players
;   - if players odd: only of by one
;   - no empty team
; - game state / level transition in order
; - require game level after joining is done
; - words-per-player >= 1

(def ex {(uuid) {:teams [{:name "Foo"
                          :private-uuid (uuid)
                          :public-uuid (uuid)
                          :players [{:name "Guy"
                                     :private-uuid (uuid)
                                     :public-uuid (uuid)
                                     :leader? true}]}
                         :points {:sentences 18
                                  :word 32}
                         {:name "Bar"
                          :private-uuid (uuid)
                          :public-uuid (uuid)
                          :players [{:name "Other guy"
                                     :private-uuid (uuid)
                                     :public-uuid (uuid)}]}] 
                 :points {:sentences 42
                          :word 12}
                 :words-per-player 5
                 :wanted-max-level :word
                 :state :playing
                 :level :sentences
                 ; Mapping from word to count (we don't want to flag multiple entries).
                 :words {"foo" 1 "bar" 1}
                 :words-round-remaining {"foo" 1}
                 :current-player "public-player-uuid"
                 ; We send the round-start-time-nanos and the server time for clients refreshing the page.
                 ; This should be correct, as nanoTime should be a monotonic clock.
                 :round-start-time-nanos (java.lang.System/nanoTime)
                 :current-word "bar"}})

(spec/def :player/name string?)
(spec/def :player/leader? boolean?)
(spec/def :hutspiel/player (spec/keys :req [:player/name :uuid/public :uuid/private]
                                      :opt [:player/leader?]))

(defn create-player [name leader?] {:player/name name
                                    :uuid/public (uuid)
                                    :uuid/private (uuid)
                                    :player/leader? leader?})

(spec/def :team/name string?)
(spec/def :team/players (spec/coll-of :hutspiel/player
                                      :kind vector?))
(spec/def :hutspiel/team (spec/keys :req [:team/name
                                          :uuid/public
                                          :uuid/private
                                          :team/players]))

(defn create-team [name] {:team/name name
                          :uuid/public (uuid)
                          :uuid/private (uuid)
                          :team/players []})

(def game-states [:joining :ready? :playing :done])
(spec/def :game/state (set game-states))

(def game-levels [:sentences :word :mime])
(spec/def :game/level (set game-levels))

(spec/def :game/teams (spec/coll-of :hutspiel/team
                                    :min 2
                                    :kind vector?))

(spec/def :setting/words-per-player int?)
(spec/def :setting/wanted-max-level :game/level)

(spec/def :hutspiel/game (spec/keys :req [:game/teams
                                          :setting/words-per-player
                                          :setting/wanted-max-level
                                          :game/state]
                                    :opt [:game/level]))

(spec/def :hutspiel/state (spec/map-of uuid? :hutspiel/game))

(defn create-game [state settings]
  (let [team-names (:setting/team-names settings ["Team 1" "Team 2"])
        teams-without-player (mapv create-team team-names)
        player (create-player (:setting/player-name settings) true)
        teams (assoc-in teams-without-player [0 :players] [player])
        selected-settings (select-keys settings [:setting/words-per-player
                                                 :setting/wanted-max-level])
        game (merge {:game/teams teams :game/state :joining} selected-settings)]
    (assoc state (uuid) game)))

(def example-game (create-game {} {:setting/team-names ["A" "B" "C"]
                                   :setting/words-per-player 5
                                   :setting/wanted-max-level :sentences
                                   :setting/player-name "Foo"}))

(spec/conform :hutspiel/state example-game)
