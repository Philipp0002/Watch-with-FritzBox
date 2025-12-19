/*
 * Copyright 2016 Emanuele Papa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.w3ma.m3u8parser.data;


/**
 * Created by Emanuele on 31/08/2016.
 */
public class ExtInfo {

    private String duration;
    private String tvgId;
    private String tvgName;
    private String tvgLogoUrl;
    private String groupTitle;
    private String wwfbType;
    private String title;

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getTvgId() {
        return tvgId;
    }

    public void setTvgId(String tvgId) {
        this.tvgId = tvgId;
    }

    public String getTvgName() {
        return tvgName;
    }

    public void setTvgName(String tvgName) {
        this.tvgName = tvgName;
    }

    public String getTvgLogoUrl() {
        return tvgLogoUrl;
    }

    public void setTvgLogoUrl(String tvgLogoUrl) {
        this.tvgLogoUrl = tvgLogoUrl;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    public String getWwfbType() {
        return wwfbType;
    }

    public void setWwfbType(String wwfbType) {
        this.wwfbType = wwfbType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
