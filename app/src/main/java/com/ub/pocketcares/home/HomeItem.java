/*
 * Copyright 2020 University at Buffalo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ub.pocketcares.home;

import java.util.ArrayList;

public class HomeItem {
    private ArrayList<Integer> assets;
    private ArrayList<String> descriptions;

    HomeItem(ArrayList<Integer> a, ArrayList<String> d){
        this.assets=a;
        this.descriptions=d;
    }

    public ArrayList<Integer> getAssets(){
        return this.assets;
    }

    public ArrayList<String> getDescriptions(){
        return this.descriptions;
    }
}
